/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.prelert.job.persistence;

import java.io.IOException;
import java.util.Locale;

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.xpack.prelert.job.DataCounts;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.xpack.prelert.job.persistence.JobResultsPersister.getJobIndexName;

/**
 * Update a job's dataCounts
 * i.e. the number of processed records, fields etc.
 */
public class JobDataCountsPersister extends AbstractComponent {

    private final Client client;

    public JobDataCountsPersister(Settings settings, Client client) {
        super(settings);
        this.client = client;
    }

    private XContentBuilder serialiseCounts(DataCounts counts) throws IOException {
        XContentBuilder builder = jsonBuilder();
        return counts.toXContent(builder, ToXContent.EMPTY_PARAMS);
    }

    /**
     * Update the job's data counts stats and figures.
     *
     * @param jobId Job to update
     * @param counts The counts
     */
    public void persistDataCounts(String jobId, DataCounts counts) {
        try {
            XContentBuilder content = serialiseCounts(counts);
            client.prepareIndex(getJobIndexName(jobId), DataCounts.TYPE.getPreferredName(),
                    jobId + DataCounts.DOCUMENT_SUFFIX)
            .setSource(content).execute().actionGet();
        } catch (IOException ioe) {
            logger.warn((Supplier<?>)() -> new ParameterizedMessage("[{}] Error serialising DataCounts stats", jobId), ioe);
        } catch (IndexNotFoundException e) {
            String msg = String.format(Locale.ROOT, "[%s] Error writing status stats.", jobId);
            logger.warn(msg, e);
        }
    }
}
