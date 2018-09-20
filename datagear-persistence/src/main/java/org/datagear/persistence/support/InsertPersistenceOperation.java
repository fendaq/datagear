/*
 * Copyright (c) 2018 by datagear.org.
 */

package org.datagear.persistence.support;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.datagear.model.Model;
import org.datagear.model.Property;
import org.datagear.model.features.NotReadable;
import org.datagear.model.support.MU;
import org.datagear.persistence.Dialect;
import org.datagear.persistence.SqlBuilder;
import org.datagear.persistence.UnsupportedModelCharacterException;
import org.datagear.persistence.features.AutoGenerated;
import org.datagear.persistence.features.ValueGenerator;
import org.datagear.persistence.mapper.JoinTableMapper;
import org.datagear.persistence.mapper.ModelTableMapper;
import org.datagear.persistence.mapper.PropertyModelMapper;
import org.datagear.persistence.mapper.PropertyTableMapper;
import org.datagear.persistence.mapper.RelationMapper;
import org.datagear.persistence.support.ExpressionResolver.Expression;
import org.springframework.core.convert.ConversionService;

/**
 * 插入持久化操作类。
 * 
 * @author datagear@163.com
 *
 */
public class InsertPersistenceOperation extends AbstractModelPersistenceOperation
{
	private ConversionService conversionService;

	private ExpressionResolver expressionResolver;

	public InsertPersistenceOperation()
	{
		super();
	}

	public InsertPersistenceOperation(ConversionService conversionService, ExpressionResolver expressionResolver)
	{
		super();
		this.conversionService = conversionService;
		this.expressionResolver = expressionResolver;
	}

	public ConversionService getConversionService()
	{
		return conversionService;
	}

	public void setConversionService(ConversionService conversionService)
	{
		this.conversionService = conversionService;
	}

	public ExpressionResolver getExpressionResolver()
	{
		return expressionResolver;
	}

	public void setExpressionResolver(ExpressionResolver expressionResolver)
	{
		this.expressionResolver = expressionResolver;
	}

	/**
	 * 插入对象到指定表。
	 * 
	 * @param cn
	 * @param dialect
	 * @param table
	 * @param model
	 * @param obj
	 * 
	 */
	public int insert(Connection cn, Dialect dialect, String table, Model model, Object obj)
	{
		return insert(cn, dialect, table, model, obj, null, null, null, new HashMap<String, Object>());
	}

	/**
	 * 插入属性表数据。
	 * 
	 * @param cn
	 * @param dialect
	 * @param table
	 * @param model
	 * @param obj
	 * @param property
	 * @param relationMapper
	 * @param propertyValue
	 * @param expressionValueCache
	 *            用于缓存SQL表达式求值结果的映射表
	 * @return
	 */
	public int insertPropertyTableData(Connection cn, Dialect dialect, String table, Model model, Object obj,
			Property property, Object propertyValue)
	{
		return insertPropertyTableData(cn, dialect, table, model, obj, property, getRelationMapper(model, property),
				propertyValue, new HashMap<String, Object>());
	}

	/**
	 * 插入对象到指定表。
	 * 
	 * @param cn
	 * @param dialect
	 * @param table
	 * @param model
	 * @param obj
	 * @param extraColumnNames
	 *            附加列名称数组，允许为{@code null}
	 * @param extraColumnValues
	 *            附加列值，允许为{@code null}
	 * @param ignorePropertyName
	 *            忽略的属性名称，用于处理双向关联时，允许为{@code null}
	 * @param expressionValueCache
	 *            用于缓存SQL表达式求值结果的映射表
	 * 
	 */
	protected int insert(Connection cn, Dialect dialect, String table, Model model, Object obj,
			String[] extraColumnNames, Object[] extraColumnValues, String ignorePropertyName,
			Map<String, Object> expressionValueCache)
	{
		int count = insertModelTableData(cn, dialect, table, model, obj, extraColumnNames, extraColumnValues,
				ignorePropertyName, expressionValueCache);

		insertPropertyTableData(cn, dialect, table, model, obj, ignorePropertyName, expressionValueCache);

		return count;
	}

	/**
	 * 插入模型表数据。
	 * <p>
	 * 此方法仅处理{@linkplain ModelTableMapper}数据。
	 * </p>
	 * 
	 * @param cn
	 * @param dialect
	 * @param table
	 * @param model
	 * @param obj
	 * @param extraColumnNames
	 *            附加列名称数组，允许为{@code null}
	 * @param extraColumnValues
	 *            附加列值，允许为{@code null}
	 * @param ignorePropertyName
	 *            忽略的属性名称，允许为{@code null}
	 * @param expressionValueCache
	 *            用于缓存SQL表达式求值结果的映射表
	 * 
	 */
	protected int insertModelTableData(Connection cn, Dialect dialect, String table, Model model, Object obj,
			String[] extraColumnNames, Object[] extraColumnValues, String ignorePropertyName,
			Map<String, Object> expressionValueCache)
	{
		Property[] properties = model.getProperties();
		Object[] propertyValues = MU.getPropertyValues(model, obj, properties);

		// 先求得SQL表达式属性值并赋予obj，因为某些驱动程序并不支持任意设置Statement.getGeneratedKeys()
		for (int i = 0; i < properties.length; i++)
		{
			Property property = properties[i];

			if (isInsertModelTableDataIgnoreProperty(model, property, ignorePropertyName))
				continue;

			if (MU.isMultipleProperty(property))
				continue;

			Object propertyValue = propertyValues[i];

			List<Expression> expressions = this.expressionResolver.resolve(propertyValue);
			if (expressions != null && !expressions.isEmpty())
			{
				propertyValue = evaluatePropertyValueForQueryExpressions(cn, model, property, (String) propertyValue,
						expressions, expressionValueCache, this.conversionService, this.expressionResolver);

				propertyValues[i] = propertyValue;
				property.set(obj, propertyValue);
			}
		}

		List<Property> autoGeneratedProperties = new ArrayList<Property>();
		List<String> autoGeneratedPropertyNames = new ArrayList<String>();

		List<String> insertColumnNames = new ArrayList<String>();
		List<Object> insertColumnValues = new ArrayList<Object>();

		ModelOrderGenerator modelOrderGenerator = new ModelOrderGenerator()
		{
			@Override
			public long generate(Model model, Property property,
					PropertyModelMapper<ModelTableMapper> propertyModelMapper, Object propertyValue,
					Object[] propertyKeyColumnValues)
			{
				// TODO 实现排序值生成逻辑
				return 0;
			}
		};

		SqlBuilder sql = SqlBuilder.valueOf().sql("INSERT INTO ").sql(toQuoteName(dialect, table)).sql(" (")
				.delimit(",");
		SqlBuilder valueSql = SqlBuilder.valueOf().sql(" VALUES (").delimit(",");

		for (int i = 0; i < properties.length; i++)
		{
			Property property = properties[i];

			if (isInsertModelTableDataIgnoreProperty(model, property, ignorePropertyName))
				continue;

			Object propValue = propertyValues[i];
			RelationMapper relationMapper = getRelationMapper(model, property);
			PropertyModelMapper<?>[] propertyModelMappers = PropertyModelMapper.valueOf(property, relationMapper);

			if (propValue == null)
			{
				// 即使自增长列，数据库也是允许自定义值的
				if (property.hasFeature(AutoGenerated.class))
				{
					if (!MU.isConcretePrimitiveProperty(property) || MU.isMultipleProperty(property))
						throw new UnsupportedModelCharacterException(
								"[" + model + "] 's [" + property + "] has [" + AutoGenerated.class.getSimpleName()
										+ "] feature, it must be single, concrete and primitive.");

					String columnName = propertyModelMappers[0].castModelTableMapperInfo().getMapper()
							.getPrimitiveColumnName();

					autoGeneratedProperties.add(property);
					autoGeneratedPropertyNames.add(columnName);
				}
				else
				{
					// null值不写入SQL语句，这样可以让数据库去处理SQL级的默认值
				}
			}
			else
			{
				addColumnNames(model, property, propertyModelMappers, true, true, true, insertColumnNames);
				addColumnValues(cn, model, property, propertyModelMappers, propValue, true, modelOrderGenerator, true,
						insertColumnValues);
			}
		}

		if (extraColumnNames != null)
		{
			addArrayToList(insertColumnNames, extraColumnNames);
			addArrayToList(insertColumnValues, extraColumnValues);
		}

		valueSql.sqld("?", insertColumnValues.size()).sql(") ").arg(toObjectArray(insertColumnValues));
		sql.sqld(toQuoteNames(dialect, toStringArray(insertColumnNames))).sql(")").sql(valueSql);

		return executeUpdateForGeneratedProperties(cn, sql, model, autoGeneratedProperties, autoGeneratedPropertyNames,
				obj);
	}

	/**
	 * 插入属性表数据。
	 * 
	 * @param cn
	 * @param dialect
	 * @param table
	 * @param model
	 * @param obj
	 * @param ignorePropertyName
	 *            忽略的属性名称，用于处理双向关联时，允许为{@code null}
	 * @param expressionValueCache
	 *            用于缓存SQL表达式求值结果的映射表
	 */
	protected void insertPropertyTableData(Connection cn, Dialect dialect, String table, Model model, Object obj,
			String ignorePropertyName, Map<String, Object> expressionValueCache)
	{
		Property[] properties = model.getProperties();

		for (int i = 0; i < properties.length; i++)
		{
			Property property = properties[i];
			Object propertyValue = MU.getPropertyValue(model, obj, property);

			if (propertyValue == null)
				continue;

			if (ignorePropertyName != null && ignorePropertyName.equals(property.getName()))
				continue;

			insertPropertyTableData(cn, dialect, table, model, obj, property, getRelationMapper(model, property),
					propertyValue, expressionValueCache);
		}
	}

	/**
	 * 插入属性表数据。
	 * 
	 * @param cn
	 * @param dialect
	 * @param table
	 * @param model
	 * @param obj
	 * @param property
	 * @param relationMapper
	 * @param propertyValue
	 * @param expressionValueCache
	 *            用于缓存SQL表达式求值结果的映射表
	 * @return
	 */
	protected int insertPropertyTableData(Connection cn, Dialect dialect, String table, Model model, Object obj,
			Property property, RelationMapper relationMapper, Object propertyValue,
			Map<String, Object> expressionValueCache)
	{
		int count = 0;

		if (this.expressionResolver.isExpression(propertyValue))
		{
			return PERSISTENCE_IGNORED;
		}
		if (MU.isSingleProperty(property))
		{
			PropertyModelMapper<?> propertyModelMapper = PropertyModelMapper.valueOf(property, relationMapper,
					propertyValue);

			return insertPropertyTableData(cn, dialect, table, model, obj, property, propertyModelMapper,
					toArray(propertyValue), null, expressionValueCache);
		}
		else
		{
			Object[] propValueAry = toArray(propertyValue);

			Model[] propertyModels = MU.getModels(property);

			List<IndexValue<Object>>[] modeledPropValues = sortByModel(propertyModels, propValueAry);

			for (int i = 0; i < propertyModels.length; i++)
			{
				Model propertyModel = propertyModels[i];
				List<IndexValue<Object>> myModeledPropValueList = modeledPropValues[i];

				Object[] myPropValues = IndexValue.toValueArray(myModeledPropValueList,
						new Object[myModeledPropValueList.size()]);
				long[] myPropValueIdexes = IndexValue.toIndexArray(myModeledPropValueList);

				int myCount = insertPropertyTableData(cn, dialect, table, model, obj, property,
						PropertyModelMapper.valueOf(property, relationMapper, propertyModel), myPropValues,
						myPropValueIdexes, expressionValueCache);

				if (myCount > 0)
					count += myCount;
			}
		}

		return count;
	}

	/**
	 * 插入属性表数据。
	 * 
	 * @param cn
	 * @param dialect
	 * @param table
	 * @param model
	 * @param obj
	 * @param property
	 * @param propertyModelMapper
	 * @param propValues
	 * @param propValueOrders
	 *            允许为{@code null}
	 * @param expressionValueCache
	 *            用于缓存SQL表达式求值结果的映射表
	 */
	protected int insertPropertyTableData(Connection cn, Dialect dialect, String table, Model model, Object obj,
			Property property, PropertyModelMapper<?> propertyModelMapper, Object[] propValues, long[] propValueOrders,
			Map<String, Object> expressionValueCache)
	{
		if (propertyModelMapper.isModelTableMapperInfo())
		{
			PropertyModelMapper<ModelTableMapper> mpmm = propertyModelMapper.castModelTableMapperInfo();
			ModelTableMapper modelTableMapper = mpmm.getMapper();

			if (modelTableMapper.isPrimitivePropertyMapper())
				return PERSISTENCE_IGNORED;

			if (PMU.isShared(model, property, propertyModelMapper.getModel()))
				return PERSISTENCE_IGNORED;

			if (propValues.length != 1)
				throw new IllegalArgumentException();

			return insertPropertyTableDataForCompositeModelTableMapper(cn, dialect, table, model, obj, property, mpmm,
					propValues[0], expressionValueCache);
		}
		else if (propertyModelMapper.isPropertyTableMapperInfo())
		{
			PropertyModelMapper<PropertyTableMapper> ppmm = propertyModelMapper.castPropertyTableMapperInfo();

			return insertPropertyTableDataForPropertyTableMapper(cn, dialect, table, model, obj, property, ppmm,
					propValues, propValueOrders, expressionValueCache);
		}
		else if (propertyModelMapper.isJoinTableMapperInfo())
		{
			PropertyModelMapper<JoinTableMapper> jpmm = propertyModelMapper.castJoinTableMapperInfo();

			return insertPropertyTableDataForJoinTableMapper(cn, dialect, table, model, obj, property, jpmm, propValues,
					propValueOrders, expressionValueCache);
		}
		else
			throw new UnsupportedOperationException();
	}

	/**
	 * 插入属性表数据。
	 * 
	 * @param cn
	 * @param dialect
	 * @param table
	 * @param model
	 * @param obj
	 * @param property
	 * @param propertyModelMapper
	 * @param propValue
	 * @param expressionValueCache
	 *            用于缓存SQL表达式求值结果的映射表
	 * 
	 */
	protected int insertPropertyTableDataForCompositeModelTableMapper(Connection cn, Dialect dialect, String table,
			Model model, Object obj, Property property, PropertyModelMapper<ModelTableMapper> propertyModelMapper,
			Object propValue, Map<String, Object> expressionValueCache)
	{
		Model propertyModel = propertyModelMapper.getModel();

		if (!PMU.isPrivate(model, property, propertyModel))
			return PERSISTENCE_IGNORED;

		return insert(cn, dialect, getTableName(propertyModel), propertyModel, propValue, null, null, null,
				expressionValueCache);
	}

	/**
	 * 插入属性表数据。
	 * 
	 * @param cn
	 * @param dialect
	 * @param table
	 * @param model
	 * @param obj
	 * @param property
	 * @param propertyModelMapper
	 * @param propValues
	 * @param propValueOrders
	 *            允许为{@code null}
	 * @param expressionValueCache
	 *            用于缓存SQL表达式求值结果的映射表
	 * @return
	 */
	protected int insertPropertyTableDataForPropertyTableMapper(Connection cn, Dialect dialect, String table,
			Model model, Object obj, Property property, PropertyModelMapper<PropertyTableMapper> propertyModelMapper,
			Object[] propValues, long[] propValueOrders, Map<String, Object> expressionValueCache)
	{
		PropertyTableMapper mapper = propertyModelMapper.getMapper();

		if (mapper.isPrimitivePropertyMapper())
		{
			return insertPropertyTableDataForPrimitiveValuePropertyTableMapper(cn, dialect, table, model, obj, property,
					propertyModelMapper, propValues, propValueOrders, expressionValueCache);
		}
		else
		{
			return insertPropertyTableDataForCompositePropertyTableMapper(cn, dialect, table, model, obj, property,
					propertyModelMapper, propValues, propValueOrders, expressionValueCache);
		}
	}

	/**
	 * 插入属性表数据。
	 * 
	 * @param cn
	 * @param dialect
	 * @param table
	 * @param model
	 * @param obj
	 * @param property
	 * @param propertyModelMapper
	 * @param propValues
	 * @param propValueOrders
	 *            允许为{@code null}
	 * @param expressionValueCache
	 *            用于缓存SQL表达式求值结果的映射表
	 * 
	 */
	protected int insertPropertyTableDataForPrimitiveValuePropertyTableMapper(Connection cn, Dialect dialect,
			String table, Model model, Object obj, Property property,
			PropertyModelMapper<PropertyTableMapper> propertyModelMapper, Object[] propValues, long[] propValueOrders,
			Map<String, Object> expressionValueCache)
	{
		int count = 0;

		PropertyTableMapper mapper = propertyModelMapper.getMapper();

		Object[] modelKeyColumnValues = getModelKeyColumnValues(cn, mapper, model, obj);

		String tableName = toQuoteName(dialect, mapper.getPrimitiveTableName());
		String[] modelKeyColumnNames = toQuoteNames(dialect, mapper.getModelKeyColumnNames());
		String valueColumnName = toQuoteName(dialect, mapper.getPrimitiveColumnName());

		SqlBuilder sql = SqlBuilder.valueOf();

		sql.sql("INSERT INTO ").sql(toQuoteName(dialect, tableName)).sql(" (").delimit(",");
		sql.sqld(modelKeyColumnNames).sqld(valueColumnName);
		if (mapper.hasModelConcreteColumn())
			sql.sqld(toQuoteName(dialect, mapper.getModelConcreteColumnName()));
		if (mapper.hasPropertyOrderColumn())
			sql.sqld(toQuoteName(dialect, mapper.getPropertyOrderColumnName()));
		sql.sql(") VALUES(").delimit(",");
		sql.sqld("?", modelKeyColumnNames.length).sqld("?");
		if (mapper.hasModelConcreteColumn())
			sql.sqld("?");
		if (mapper.hasPropertyOrderColumn())
			sql.sqld("?");
		sql.sql(")");

		for (int i = 0; i < propValues.length; i++)
		{
			Object propertyValue = propValues[i];

			List<Expression> expressions = this.expressionResolver.resolve(propertyValue);
			if (expressions != null && !expressions.isEmpty())
			{
				propertyValue = evaluatePropertyValueForQueryExpressions(cn, model, property, (String) propertyValue,
						expressions, expressionValueCache, this.conversionService, this.expressionResolver);
			}

			Object columnValue = getColumnValue(cn, model, property, propertyModelMapper, propertyValue);

			sql.arg(modelKeyColumnValues).arg(columnValue);

			if (mapper.hasModelConcreteColumn())
				sql.arg(mapper.getModelConcreteColumnValue());

			if (mapper.hasPropertyOrderColumn())
				sql.arg((propValueOrders == null ? i : propValueOrders[i]));

			count += executeUpdate(cn, sql);

			sql.resetArg();
		}

		return count;
	}

	/**
	 * 插入属性表数据。
	 * 
	 * @param cn
	 * @param dialect
	 * @param table
	 * @param model
	 * @param obj
	 * @param property
	 * @param propertyModelMapper
	 * @param propValues
	 * @param propValueOrders
	 *            允许为{@code null}
	 * @param expressionValueCache
	 *            用于缓存SQL表达式求值结果的映射表
	 * @return
	 */
	protected int insertPropertyTableDataForCompositePropertyTableMapper(Connection cn, Dialect dialect, String table,
			Model model, Object obj, Property property, PropertyModelMapper<PropertyTableMapper> propertyModelMapper,
			Object[] propValues, long[] propValueOrders, Map<String, Object> expressionValueCache)
	{
		int count = 0;

		Object[] modelKeyColumnValues = getModelKeyColumnValues(cn, propertyModelMapper.getMapper(), model, obj);

		Model propertyModel = propertyModelMapper.getModel();
		PropertyTableMapper mapper = propertyModelMapper.getMapper();

		String ptable = getTableName(propertyModel);

		if (PMU.isPrivate(model, property, propertyModel))
		{
			String[] allMapperColumNames = getPropertyTableMapperAllColumnNames(mapper);

			for (int i = 0; i < propValues.length; i++)
			{
				Object[] allMapperColumnValues = getPropertyTableMapperAllColumnValues(mapper, modelKeyColumnValues,
						(propValueOrders == null ? null : propValueOrders[i]));

				int myCount = insert(cn, dialect, ptable, propertyModel, propValues[i], allMapperColumNames,
						allMapperColumnValues, getMappedByWith(mapper), expressionValueCache);

				if (myCount > 0)
					count += myCount;
			}
		}
		else
		{
			Object[][] pidColumnValues = getIdColumnValuesForObj(cn, propertyModel, propValues);

			count = updateRelationForCompositePropertyTableMapper(cn, dialect, model, obj, property,
					propertyModelMapper, modelKeyColumnValues, pidColumnValues, propValueOrders);
		}

		return count;
	}

	/**
	 * 为复合属性{@linkplain PropertyTableMapper}更新属性表内的关联关系。
	 * 
	 * @param cn
	 * @param dialect
	 * @param model
	 * @param obj
	 * @param property
	 * @param propertyModelMapper
	 * @param modelKeyColumnValues
	 * @param propValueEntityIdColumnValues
	 * @param propValueEleOrders
	 *            实体属性值数组对应元素的排序值，如果{@code mapper}的
	 *            {@linkplain PropertyTableMapper#hasPropertyOrderColumn()}为
	 *            {@code false}，此参数被忽略；否则，不能为{@code null}且必须与
	 *            {@code propValueEntityIdColumnValues}一一对应
	 * @return
	 */
	protected int updateRelationForCompositePropertyTableMapper(Connection cn, Dialect dialect, Model model, Object obj,
			Property property, PropertyModelMapper<PropertyTableMapper> propertyModelMapper,
			Object[] modelKeyColumnValues, Object[][] propValueEntityIdColumnValues, long[] propValueEleOrders)
	{
		int count = 0;

		Model propertyModel = propertyModelMapper.getModel();
		PropertyTableMapper mapper = propertyModelMapper.getMapper();

		String tableName = toQuoteName(dialect, getTableName(propertyModel));
		String[] pidColNames = toQuoteNames(dialect, getIdColumnNames(propertyModel));
		String[] modelkeyColumnNames = toQuoteNames(dialect, mapper.getModelKeyColumnNames());

		SqlBuilder sql = SqlBuilder.valueOf();

		sql.sql("UPDATE ").sql(tableName).sql(" SET ").delimit(",");
		sql.sqldSuffix(modelkeyColumnNames, "=?");

		if (mapper.hasModelConcreteColumn())
			sql.sqldSuffix(mapper.getModelConcreteColumnName(), "=?");

		if (mapper.hasPropertyOrderColumn())
			sql.sqldSuffix(mapper.getPropertyOrderColumnName(), "=?");

		sql.sql(" WHERE ").delimit("AND ");
		sql.sqldSuffix(pidColNames, "=?");

		for (int i = 0; i < propValueEntityIdColumnValues.length; i++)
		{
			Object[] pidColumnValue = propValueEntityIdColumnValues[i];

			sql.arg(modelKeyColumnValues);

			if (mapper.hasModelConcreteColumn())
				sql.arg(mapper.getModelConcreteColumnValue());

			if (mapper.hasPropertyOrderColumn())
				sql.arg(propValueEleOrders[i]);

			sql.arg(pidColumnValue);

			count += executeUpdate(cn, sql);

			sql.resetArg();
		}

		return count;
	}

	/**
	 * 插入属性表数据。
	 * 
	 * @param cn
	 * @param dialect
	 * @param table
	 * @param model
	 * @param obj
	 * @param property
	 * @param propertyModelMapper
	 * @param propValues
	 * @param propValueOrders
	 *            允许为{@code null}
	 * @param expressionValueCache
	 *            用于缓存SQL表达式求值结果的映射表
	 * 
	 */
	protected int insertPropertyTableDataForJoinTableMapper(Connection cn, Dialect dialect, String table, Model model,
			Object obj, Property property, PropertyModelMapper<JoinTableMapper> propertyModelMapper,
			Object[] propValues, long[] propValueOrders, Map<String, Object> expressionValueCache)
	{
		Model propertyModel = propertyModelMapper.getModel();

		String ptable = getTableName(propertyModel);

		for (int i = 0; i < propValues.length; i++)
		{
			if (PMU.isPrivate(model, property, propertyModel))
			{
				insert(cn, dialect, ptable, propertyModel, propValues[i], null, null, null, expressionValueCache);
			}
		}

		return insertPropertyTableDataRelationForJoinTableMapper(cn, dialect, table, model, obj, property,
				propertyModelMapper, propValues, propValueOrders);
	}

	/**
	 * 插入属性表数据的关联关系。
	 * 
	 * @param cn
	 * @param dialect
	 * @param table
	 * @param model
	 * @param obj
	 * @param property
	 * @param propertyModelMapper
	 * @param propValues
	 * @param propValueOrders
	 *            允许为{@code null}
	 * 
	 */
	protected int insertPropertyTableDataRelationForJoinTableMapper(Connection cn, Dialect dialect, String table,
			Model model, Object obj, Property property, PropertyModelMapper<JoinTableMapper> propertyModelMapper,
			Object[] propValues, long[] propValueOrders)
	{
		int count = 0;

		JoinTableMapper mapper = propertyModelMapper.getMapper();

		String tableName = toQuoteName(dialect, mapper.getJoinTableName());
		String[] mkeyColumnNames = toQuoteNames(dialect, mapper.getModelKeyColumnNames());
		String[] pkeyColumnNames = toQuoteNames(dialect, mapper.getPropertyKeyColumnNames());

		Object[] modelKeyColumnValues = getModelKeyColumnValues(cn, mapper, model, obj);

		SqlBuilder sql = SqlBuilder.valueOf();

		sql.sql("INSERT INTO ").sql(tableName).sql(" (").delimit(",");
		sql.sqld(mkeyColumnNames).sqld(pkeyColumnNames);
		if (mapper.hasModelConcreteColumn())
			sql.sqld(toQuoteName(dialect, mapper.getModelConcreteColumnName()));
		if (mapper.hasPropertyConcreteColumn())
			sql.sqld(toQuoteName(dialect, mapper.getPropertyConcreteColumnName()));
		if (mapper.hasModelOrderColumn())
			sql.sqld(toQuoteName(dialect, mapper.getModelOrderColumnName()));
		if (mapper.hasPropertyOrderColumn())
			sql.sqld(toQuoteName(dialect, mapper.getPropertyOrderColumnName()));
		sql.sql(") VALUES(");
		sql.sqld("?", mkeyColumnNames.length + pkeyColumnNames.length);
		if (mapper.hasModelConcreteColumn())
			sql.sqld("?");
		if (mapper.hasPropertyConcreteColumn())
			sql.sqld("?");
		if (mapper.hasModelOrderColumn())
			sql.sqld("?");
		if (mapper.hasPropertyOrderColumn())
			sql.sqld("?");
		sql.sql(")");

		for (int i = 0; i < propValues.length; i++)
		{
			Object[] pkeyColumnValues = getPropertyKeyColumnValues(cn, mapper, propertyModelMapper.getModel(),
					propValues[i]);

			sql.arg(modelKeyColumnValues).arg(pkeyColumnValues);
			if (mapper.hasModelConcreteColumn())
				sql.arg(mapper.getModelConcreteColumnValue());
			if (mapper.hasPropertyConcreteColumn())
				sql.arg(mapper.getPropertyConcreteColumnValue());
			if (mapper.hasModelOrderColumn())
			{
				// TODO 查询模型端最大排序值
				long morder = 0;
				// getMaxModelOrderForJoinTableMapper(model, obj,
				// idColumnValues, property,
				// propertyConcreteModel, propertyConcreteModelIdx,
				// relationMapper, mapper, propValues[i]);

				sql.arg(morder++);
			}

			if (mapper.hasPropertyOrderColumn())
				sql.arg((propValueOrders == null ? i : propValueOrders[i]));

			count += executeUpdate(cn, sql);

			sql.resetArg();
		}

		return count;
	}

	/**
	 * 获取所有属性值，并且在值为{@code null}时使用{@linkplain ValueGenerator}生成。
	 * 
	 * @param model
	 * @param obj
	 * @return
	 */
	protected Object[] getPropertyValuesWithGenerator(Model model, Object obj)
	{
		Property[] properties = model.getProperties();

		Object[] pvs = new Object[properties.length];

		for (int i = 0; i < properties.length; i++)
			pvs[i] = getPropertyValueWithGenerator(model, properties[i], obj);

		return pvs;
	}

	/**
	 * 获取属性值，并且在值为{@code null}时使用{@linkplain ValueGenerator}生成。
	 * 
	 * @param model
	 * @param property
	 * @param obj
	 * @return
	 */
	protected Object getPropertyValueWithGenerator(Model model, Property property, Object obj)
	{
		Object propValue = MU.getPropertyValue(model, obj, property);

		if (propValue == null)
		{
			propValue = property.getDefaultValue();

			if (propValue == null)
			{
				ValueGenerator valueGenerator = property.getFeature(ValueGenerator.class);
				if (valueGenerator != null)
					propValue = valueGenerator.generate(model, property, obj);

				if (propValue != null && this.expressionResolver.isExpression(propValue))
					property.set(obj, propValue);
			}
		}

		return propValue;
	}

	/**
	 * 是否是插入模型表时的忽略属性。
	 * 
	 * @param model
	 * @param property
	 * @param ignorePropertyName
	 * @return
	 */
	protected boolean isInsertModelTableDataIgnoreProperty(Model model, Property property, String ignorePropertyName)
	{
		if (MU.isMultipleProperty(property))
			return true;

		if (property.hasFeature(NotReadable.class))
			return true;

		if (ignorePropertyName != null && ignorePropertyName.equals(property.getName()))
			return true;

		return false;
	}
}
