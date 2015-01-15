/***************************************************
 * dynaTrace Diagnostics (c) dynaTrace software GmbH
 *
 * @file: Measure.java
 * @date: 13.03.2013
 * @author: cwat-dstadler
 */
package com.dynatrace.diagnostics.plugins.amazon;

import java.util.HashMap;
import java.util.Map;


/**
 * Helper class to handle measures together with their dynamic measure values locally.
 *
 * These are then passed on to the dynaTrace measure interface.
 *
 * This class supports only one type of dynamic measure which can have a number of
 * values reported for different occurrences.
 *
 * It also supports an "adjustmentFactor", i.e. a constant value which is applied
 * to the base value and also all dynamic values whenever the value is queries
 * via getValue() or getDynamicMeasures().
 *
 * @author cwat-dstadler
 */
public class Measure {
	private double value;
	private double adjustmentFactor = 1;

	private String dynamicMeasureName;
	private Map<String, Double> dynamicMeasures = new HashMap<String, Double>();

	public Measure() {
		super();
	}

	public Measure(double value) {
		this(null, value);
	}

	public Measure(String dynamicMeasureName) {
		this(dynamicMeasureName, 0);
	}

	public Measure(String dynamicMeasureName, double value) {
		this.dynamicMeasureName = dynamicMeasureName;
		this.value = value;
	}

	public void setAdjustmentFactor(double adjustmentFactor) {
		this.adjustmentFactor = adjustmentFactor;
	}

	public double getValue() {
		return value*adjustmentFactor;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public String getDynamicMeasureName() {
		return dynamicMeasureName;
	}

	public Map<String, Double> getDynamicMeasures() {
		Map<String, Double> adjustedMap = new HashMap<String, Double>();
		for(Map.Entry<String, Double> entry : dynamicMeasures.entrySet()) {
			adjustedMap.put(entry.getKey(), entry.getValue()*adjustmentFactor);
		}
		return adjustedMap;
	}

	public void addDynamicMeasure(String dynamic, double lvalue) {
		if(!dynamicMeasures.containsKey(dynamic)) {
			dynamicMeasures.put(dynamic, lvalue);
		} else {
			dynamicMeasures.put(dynamic, dynamicMeasures.get(dynamic) + lvalue);
		}
	}

	public void incValue() {
		value++;
	}

	public void addValue(double lvalue) {
		value+=lvalue;
	}
}
