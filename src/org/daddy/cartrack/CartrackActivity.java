package org.daddy.cartrack;

import static org.daddy.cartrack.Constants.BODY;
import static org.daddy.cartrack.Constants.DATE;
import static org.daddy.cartrack.Constants.DERIVED_POSITION;
import static org.daddy.cartrack.Constants.ID;

import java.util.HashMap;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class CartrackActivity extends ListActivity {
	private static String[] FROM = { DATE, DERIVED_POSITION, };
	private static int[] TO = { R.id.date, R.id.title, };

	private void showEvents(Cursor cursor) {
		// Set up data binding
		@SuppressWarnings("deprecation")
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.item, cursor, FROM, TO);
		setListAdapter(adapter);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Cursor c = getCursor();
		showEvents(c);
	}

	private Cursor getCursor() {
		// See:
		// http://pulse7.net/android/read-sms-message-inbox-sent-draft-android/
		Uri inboxURI = Uri.parse("content://sms/inbox");
		String[] reqCols = new String[] { ID, DATE, BODY };
		ContentResolver cr = getContentResolver();
		Cursor c = cr.query(inboxURI, reqCols, "body like '%Pos%'", null, null);
		return new MyCursorWrapper(c);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Uri inboxURI = Uri.parse("content://sms/inbox");
		String[] reqCols = new String[] { BODY };
		ContentResolver cr = getContentResolver();
		Cursor c = cr.query(inboxURI, reqCols, ID + "=" + id, null, null);
		c.moveToFirst();
		String body = c.getString(c.getColumnIndex(BODY));
		c.close();

		openUri(getUrl(body));
	}

	private String getUrl(String body) {
		String latitudine = new LatitudeExtractor().getValue(body);
		String longitudine = new LongitudeExtractor().getValue(body);
		return String.format("http://www.maps.google.com/maps?q=%s,%s",
				latitudine, longitudine);
	}

	private void openUri(String url) {
		Uri uri = Uri.parse(url);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private static class MyCursorWrapper extends CursorWrapper {

		private HashMap<Integer, String> additionalColsIS;
		private HashMap<String, Integer> additionalColsSI;
		private HashMap<Integer, Extractor> extractors;

		public MyCursorWrapper(Cursor cursor) {
			super(cursor);
			additionalColsIS = new HashMap<Integer, String>();
			additionalColsSI = new HashMap<String, Integer>();
			extractors = new HashMap<Integer, Extractor>();
			add(getColumnCount() + 1, Constants.DERIVED_LATITUDE,
					new LatitudeExtractor());
			add(getColumnCount() + 2, Constants.DERIVED_LONGITUDE,
					new LongitudeExtractor());
			add(getColumnCount() + 3, Constants.DERIVED_POSITION,
					new PositionExtractor());
		}

		private void add(int columnIdx, String columnName, Extractor e) {
			additionalColsIS.put(columnIdx, columnName);
			additionalColsSI.put(columnName, columnIdx);
			extractors.put(columnIdx, e);
		}

		@Override
		public int getColumnCount() {
			return super.getColumnCount() + additionalColsIS.size();
		}

		@Override
		public int getColumnIndex(String columnName) {
			Integer idx = additionalColsSI.get(columnName);
			if (idx == null)
				return super.getColumnIndex(columnName);
			return idx;
		}

		@Override
		public int getColumnIndexOrThrow(String columnName)
				throws IllegalArgumentException {
			Integer idx = additionalColsSI.get(columnName);
			if (idx == null)
				return super.getColumnIndexOrThrow(columnName);
			return idx;
		}

		@Override
		public String getColumnName(int columnIndex) {
			String name = additionalColsIS.get(columnIndex);
			if (name == null)
				return super.getColumnName(columnIndex);
			return name;
		}

		@Override
		public String getString(int columnIndex) {
			Extractor e = extractors.get(columnIndex);
			if (e != null) {
				return e.getValue(getString(getColumnIndex("body")));
			}
			return super.getString(columnIndex);
		}

	}

	private static interface Extractor {
		String getValue(String str);
	}

	private static class LatitudeExtractor implements Extractor {
		@Override
		public String getValue(String str) {
			for (String s : str.split("\n"))
				if (s.toLowerCase().startsWith("latitude"))
					return s.replaceAll("[A-Za-z:]", "").trim();

			return null;
		}
	}

	private static class LongitudeExtractor implements Extractor {
		@Override
		public String getValue(String str) {
			for (String s : str.split("\n"))
				if (s.toLowerCase().startsWith("longitude"))
					return s.replaceAll("[A-Za-z:]", "").trim();
			return null;
		}
	}

	private static class PositionExtractor implements Extractor {
		@Override
		public String getValue(String str) {
			for (String s : str.split("\n"))
				if (s.toLowerCase().startsWith("position"))
					return s.split(":")[1].trim();
			return null;
		}
	}
}
