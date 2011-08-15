package org.sigmah.server.endpoint.gwtrpc;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class Collector<T> implements AsyncCallback<T> {

	private T result = null;
	private boolean callbackCalled = false;
	
	@Override
	public void onFailure(Throwable caught) {
		throw new RuntimeException(caught);
	}

	@Override
	public void onSuccess(T result) {
		if(callbackCalled) {
			throw new IllegalStateException("Callback called a second time");
		}
		this.callbackCalled = true;
		this.result = result;
	}
	
	public T getResult() {
		if(!callbackCalled) {
			throw new IllegalStateException("Callback was not called");
		}
		return result;
	}

}