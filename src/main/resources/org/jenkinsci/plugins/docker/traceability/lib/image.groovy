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
import lib.LayoutTagLib
import jenkins.model.Jenkins
import hudson.model.Fingerprint
import java.io.IOException
import org.jenkinsci.plugins.docker.commons.fingerprint.DockerFingerprints
import org.apache.commons.lang.StringUtils

l=namespace(LayoutTagLib)
t=namespace("/lib/hudson")
st=namespace("jelly:stapler")
f=namespace("lib/form")

st.documentation() {
    text("Renders image by its Id. The rendering relies on fingerprints.")
    st.attribute(name: "id", use: "required") {
      text("Image Id. Only full 64-symbol IDs are supported. Nulls will be processed as well") 
    }
}

Fingerprint fp = null;
if (StringUtils.isNotBlank(id)) {
  try {
     fp = DockerFingerprints.of(id); 
  } catch (IOException ex) {
      // Do nothing
  }
}

if (fp != null) {
    a (href: Jenkins.instance.rootUrl+"docker-traceability/image?id="+id) {
      text(id);
    }
    if (fp.original != null) {
      text(" (from ")
      t.buildLink(job: fp.original.job, number: fp.original.number, jobName: fp.original.name)
      text(")")
    } else {
      text(" (outside Jenkins)")
    }
} else {
    text(id+ " (unregistered)");
}

