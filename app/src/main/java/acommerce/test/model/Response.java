package acommerce.test.model;

import java.util.List;

public class Response {
	private List<Result> results;

	public Response(List<Result> results) {
		this.results = results;
	}

	public List<Result> getResults() {
		return results;
	}

	public void setResults(List<Result> results) {
		this.results = results;
	}
}