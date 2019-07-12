package org.nuxeo.migration.test.operation.document.reindex;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.migration.operation.document.reindex.ReindexOperation;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.migration.operation.document.export.nuxeo-cli-new-core")
public class TestReindexOperation {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Test
    public void shouldCallTheOperation() throws OperationException {
        OperationContext ctx = new OperationContext(session);
        DocumentModel doc = (DocumentModel) automationService.run(ctx, ReindexOperation.ID);
        assertEquals("/", doc.getPathAsString());
    }
    /*
     * @Test public void shouldCallWithParameters() throws OperationException {
     * final String path = "/default-domain"; OperationContext ctx = new
     * OperationContext(session); Map<String, Object> params = new HashMap<>();
     * params.put("path", path); DocumentModel doc = (DocumentModel)
     * automationService.run(ctx, ReindexOperation.ID, params);
     * assertEquals(path, doc.getPathAsString()); }
     */
}
