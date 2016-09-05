/**
 * Licensed to DigitalPebble Ltd under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * DigitalPebble licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.digitalpebble.stormcrawler.solr.persistence;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.stormcrawler.Metadata;
import com.digitalpebble.stormcrawler.persistence.AbstractStatusUpdaterBolt;
import com.digitalpebble.stormcrawler.persistence.Status;
import com.digitalpebble.stormcrawler.solr.SolrConnection;
import com.digitalpebble.stormcrawler.util.ConfUtils;
import com.digitalpebble.stormcrawler.util.URLUtil;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;

public class StatusUpdaterBolt extends AbstractStatusUpdaterBolt {

    private static final Logger LOG = LoggerFactory
            .getLogger(StatusUpdaterBolt.class);

    private static final String BOLT_TYPE = "status";

    private static final String SolrIndexCollection = "solr.status.collection";

    private static final String SolrMetadataPrefix = "solr.status.metadata.prefix";

    private String collection;

    private String mdPrefix;

    private SolrConnection connection;

    @Override
    public void prepare(Map stormConf, TopologyContext context,
            OutputCollector collector) {

        super.prepare(stormConf, context, collector);

        collection = ConfUtils.getString(stormConf, SolrIndexCollection,
                "status");

        mdPrefix = ConfUtils.getString(stormConf, SolrMetadataPrefix,
                "metadata");

        try {
            connection = SolrConnection.getConnection(stormConf, BOLT_TYPE);
        } catch (Exception e) {
            LOG.error("Can't connect to Solr: {}", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }

    @Override
    public void store(String url, Status status, Metadata metadata,
            Date nextFetch) throws Exception {

        SolrInputDocument doc = new SolrInputDocument();

        doc.setField("url", url);

        doc.setField("host", URLUtil.getHost(url));

        doc.setField("status", status);

        Iterator<String> keyIterator = metadata.keySet().iterator();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            String[] values = metadata.getValues(key);
            doc.setField(String.format("%s.%s", mdPrefix, key), values);
        }

        doc.setField("nextFetchDate", nextFetch);

        connection.getClient().add(doc);
    }

    @Override
    public void cleanup() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                LOG.error("Can't close connection to Solr: {}", e);
            }
        }
    }
}
