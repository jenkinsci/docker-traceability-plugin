/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.docker.traceability.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.jenkinsci.plugins.docker.traceability.samples.JSONSamples;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * Tests for {@link DockerTraceabilityReport}.
 * @author Oleg Nenashev
 */
public class DockerTraceabilityReportTest {
    
    @Test
    public void jsonRoundTrip() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        DockerTraceabilityReport report = JSONSamples.submitEvent.readObject(DockerTraceabilityReport.class);
        assertNotNull(report.getContainer());
        assertNotNull(report.getEvent());
        assertNotNull(report.getHostInfo());
        assertNotNull(report.getImageId());
        assertNotNull(report.getParents());
        //TODO: content checks
        
        String value1 = mapper.writeValueAsString(report);
        DockerTraceabilityReport report2 = mapper.readValue(value1, DockerTraceabilityReport.class);
        String value2 = mapper.writeValueAsString(report2);
        assertEquals("JSONs must be equal after the second roundtrip", value1, value2);
    }
}
