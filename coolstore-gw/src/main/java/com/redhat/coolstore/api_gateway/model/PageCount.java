package com.redhat.coolstore.api_gateway.model;

public class PageCount  {

	public int pageCount;

	public PageCount() {

	}
	public Product(int pageCount) {
		this.pageCount = pageCount;
	}

	public int getPageCount() {
		return pageCount;
	}

	public void setPageCount(int PageCount) {
		this.PageCount = PageCount;
	}

	public String toString() {
		return ("PageCount pageCount: " + this.pageCount);
	}
}
