/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.code.or.binlog.impl.parser;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.or.binlog.ParserContext;
import com.google.code.or.binlog.impl.event.TableMapEvent;
import com.google.code.or.binlog.impl.event.UpdateRowsEvent;
import com.google.code.or.common.glossary.Pair;
import com.google.code.or.common.glossary.Row;
import com.google.code.or.io.XInputStream;

/**
 * 
 * @author Jingqi Xu
 */
public class UpdateRowsEventParser extends AbstractRowEventParser {
	//
	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateRowsEventParser.class);

	/**
	 * 
	 */
	public UpdateRowsEventParser() {
		super(UpdateRowsEvent.EVENT_TYPE);
	}
	
	/**
	 * 
	 */
	public void parse(XInputStream is, ParserContext context)
	throws IOException {
		//
		final long tableId = is.readLong(6);
		final TableMapEvent tme = context.getTableMapEvent(tableId);
		if(tme == null) {
			LOGGER.warn("failed to get TableMapEvent, table id: " + tableId);
			is.skip(is.available());
			return;
		} 
		
		//
		final UpdateRowsEvent event = new UpdateRowsEvent(context.getHeader());
		event.setTableId(tableId);
		event.setReserved(is.readInt(2));
		event.setColumnCount(is.readUnsignedLong()); 
		event.setUsedColumnsBefore(is.readBit(event.getColumnCount().intValue(), true));
		event.setUsedColumnsAfter(is.readBit(event.getColumnCount().intValue(), true));
		event.setRows(parseRows(is, tme, event));
		context.getListener().onEvents(event);
	}
	
	/**
	 * 
	 */
	protected List<Pair<Row>> parseRows(XInputStream is, TableMapEvent tme, UpdateRowsEvent ure)
	throws IOException {
		final List<Pair<Row>> r = new LinkedList<Pair<Row>>();
		while(is.available() > 0) {
			final Row before = parseRow(is, tme, ure.getUsedColumnsBefore());
			final Row after = parseRow(is, tme, ure.getUsedColumnsAfter());
			r.add(new Pair<Row>(before, after));
		}
		return r;
	}
}