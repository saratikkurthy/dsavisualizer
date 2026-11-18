package com.sarat.dsavisualizer.model;

import java.util.List;

/**
 * Response body for a trace endpoint: the algorithm name and the full list of
 * steps the frontend plays back.
 */
public class TraceResponse {

	private String algorithm;
	private List<Step> steps;

	public TraceResponse() {
	}

	public TraceResponse(String algorithm, List<Step> steps) {
		this.algorithm = algorithm;
		this.steps = steps;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public List<Step> getSteps() {
		return steps;
	}

	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}
}
