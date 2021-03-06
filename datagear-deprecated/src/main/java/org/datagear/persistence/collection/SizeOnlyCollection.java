/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.persistence.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.datagear.model.support.PropertyPath;

/**
 * 仅有大小信息的{@linkplain Collection}。
 * 
 * @author datagear@163.com
 *
 * @param <E>
 */
public class SizeOnlyCollection<E> implements Collection<E>
{
	public static final String SIZE_PROPERTY_NAME = "size";

	private static final PropertyPath SIZE_PROPERTY_PATH = PropertyPath.valueOf(SIZE_PROPERTY_NAME);

	private int size = 0;

	public SizeOnlyCollection()
	{
		super();
	}

	public SizeOnlyCollection(int size)
	{
		super();
		this.size = size;
	}

	public int getSize()
	{
		return size;
	}

	public void setSize(int size)
	{
		this.size = size;
	}

	@Override
	public int size()
	{
		return 0;
	}

	@Override
	public boolean isEmpty()
	{
		return true;
	}

	@Override
	public boolean contains(Object o)
	{
		return false;
	}

	@Override
	public Iterator<E> iterator()
	{
		return Collections.<E> emptyList().iterator();
	}

	@Override
	public Object[] toArray()
	{
		return new Object[0];
	}

	@Override
	public <T> T[] toArray(T[] a)
	{
		return a;
	}

	@Override
	public boolean add(E e)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c)
	{
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends E> c)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear()
	{
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " [size=" + size + "]";
	}

	/**
	 * 转换为{@linkplain Map}。
	 * 
	 * @return
	 */
	public Map<String, Integer> toMap()
	{
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put(SIZE_PROPERTY_NAME, this.size);

		return map;
	}

	/**
	 * 是否包含{@linkplain #SIZE_PROPERTY_NAME}关键字。
	 * 
	 * @param map
	 * @return
	 */
	public static boolean containsSizeKey(Map<?, ?> map)
	{
		boolean contains = map.containsKey(SIZE_PROPERTY_NAME);

		if (!contains)
			contains = map.containsKey(SIZE_PROPERTY_PATH);

		return contains;
	}

	/**
	 * 是否包含{@linkplain #SIZE_PROPERTY_NAME}关键字。
	 * 
	 * @param map
	 * @return
	 */
	public static boolean containsSizeKeyForStringMap(Map<String, ?> map)
	{
		if (map == null)
			return false;

		return map.containsKey(SIZE_PROPERTY_NAME);
	}

	/**
	 * 是否包含{@linkplain #SIZE_PROPERTY_NAME}关键字。
	 * 
	 * @param map
	 * @return
	 */
	public static boolean containsSizeKeyForPropertyPathMap(Map<PropertyPath, ?> map)
	{
		if (map == null)
			return false;

		return map.containsKey(SIZE_PROPERTY_PATH);
	}

	/**
	 * 获取{@linkplain #SIZE_PROPERTY_NAME}关键字的值。
	 * <p>
	 * 如果没有，将返回{@code null}。
	 * </p>
	 * 
	 * @param map
	 * @return
	 */
	public static Object getSizeValue(Map<?, ?> map)
	{
		Object value = map.get(SIZE_PROPERTY_NAME);

		if (value == null)
			value = map.get(SIZE_PROPERTY_PATH);

		return value;
	}

	/**
	 * 获取{@linkplain #SIZE_PROPERTY_NAME}关键字的值。
	 * <p>
	 * 如果没有，将返回{@code null}。
	 * </p>
	 * 
	 * @param map
	 * @return
	 */
	public static Object getSizeValueForStringMap(Map<String, ?> map)
	{
		if (map == null)
			return null;

		Object sizeValue = map.get(SIZE_PROPERTY_NAME);

		return sizeValue;
	}

	/**
	 * 获取{@linkplain #SIZE_PROPERTY_NAME}关键字的值。
	 * <p>
	 * 如果没有，将返回{@code null}。
	 * </p>
	 * 
	 * @param map
	 * @return
	 */
	public static Object getSizeValueForPropertyPathMap(Map<PropertyPath, ?> map)
	{
		if (map == null)
			return null;

		Object sizeValue = map.get(SIZE_PROPERTY_PATH);

		return sizeValue;
	}

	/**
	 * 移除{@linkplain #SIZE_PROPERTY_NAME}关键字。
	 * 
	 * @param map
	 */
	public static void removeSizeValue(Map<?, ?> map)
	{
		if (map == null)
			return;

		map.remove(SIZE_PROPERTY_NAME);
		map.remove(SIZE_PROPERTY_PATH);
	}

	/**
	 * 移除{@linkplain #SIZE_PROPERTY_NAME}关键字。
	 * 
	 * @param map
	 */
	public static void removeSizeValueForStringMap(Map<String, ?> map)
	{
		if (map == null)
			return;

		map.remove(SIZE_PROPERTY_NAME);
	}

	/**
	 * 移除{@linkplain #SIZE_PROPERTY_NAME}关键字。
	 * <p>
	 * 如果没有，将返回{@code null}。
	 * </p>
	 * 
	 * @param map
	 * @return
	 */
	public static void removeSizeValueForPropertyPathMap(Map<PropertyPath, ?> map)
	{
		if (map == null)
			return;

		map.remove(SIZE_PROPERTY_PATH);
	}

	/**
	 * 判断映射表是否包含且仅包含{@linkplain #SIZE_PROPERTY_NAME}关键字。
	 * 
	 * @param map
	 * @return
	 */
	public static boolean isSizeOnlyMap(Map<?, ?> map)
	{
		if (map == null)
			return false;

		if (map.size() != 1)
			return false;

		return containsSizeKey(map);
	}

	/**
	 * 判断映射表是否包含且仅包含{@linkplain #SIZE_PROPERTY_NAME}关键字。
	 * 
	 * @param map
	 * @return
	 */
	public static boolean isSizeOnlyMapForStringMap(Map<String, ?> map)
	{
		if (map == null)
			return false;

		if (map.size() != 1)
			return false;

		return containsSizeKeyForStringMap(map);
	}

	/**
	 * 判断映射表是否包含且仅包含{@linkplain #SIZE_PROPERTY_NAME}关键字。
	 * 
	 * @param map
	 * @return
	 */
	public static boolean isSizeOnlyMapForPropertyPathMap(Map<PropertyPath, ?> map)
	{
		if (map == null)
			return false;

		if (map.size() != 1)
			return false;

		return containsSizeKeyForPropertyPathMap(map);
	}

	/**
	 * 创建{@linkplain SizeOnlyCollection}实例。
	 * 
	 * @param collectionType
	 * @return
	 */
	public static SizeOnlyCollection<Object> instance(
			@SuppressWarnings("rawtypes") Class<? extends Collection> collectionType)
	{
		if (List.class.isAssignableFrom(collectionType))
			return new SizeOnlyList<Object>();
		else if (Set.class.isAssignableFrom(collectionType))
			return new SizeOnlySet<Object>();
		else if (Collection.class.isAssignableFrom(collectionType))
			return new SizeOnlyCollection<Object>();
		else
			throw new UnsupportedOperationException("Create [" + SizeOnlyCollection.class.getSimpleName() + "] for ["
					+ collectionType.getName() + "] type is not supported");
	}
}
