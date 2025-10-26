package com.example.geocache;

import java.util.concurrent.atomic.AtomicBoolean;

class Cancellation {

	public static  final class CancellationToken {
		private final AtomicBoolean cancelled = new AtomicBoolean(false);
		public boolean isCancelled(){ return cancelled.get(); }
		void cancel(){ cancelled.set(true); }
		public static CancellationToken none(){ return new CancellationToken(); }
	}
	public static final class CancellationSource {
		private final CancellationToken token = new CancellationToken();
		public CancellationToken token(){ return token; }
		public void cancel(){ token.cancel(); }
	}
}