/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.security.visibility;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.security.User;

/**
 * This is the default implementation for ScanLabelGenerator. It will extract labels passed via
 * Scan#authorizations and cross check against the global auths set for the user. The labels for which
 * user is not authenticated will be dropped even if it is passed via Scan Authorizations.
 */
@InterfaceAudience.Private
public class DefaultScanLabelGenerator implements ScanLabelGenerator {

  private static final Log LOG = LogFactory.getLog(DefaultScanLabelGenerator.class);
  
  private Configuration conf;
  
  private VisibilityLabelsManager labelsManager;
  
  public DefaultScanLabelGenerator() {
    this.labelsManager = VisibilityLabelsManager.get();
  }

  @Override
  public void setConf(Configuration conf) {
    this.conf = conf;
  }

  @Override
  public Configuration getConf() {
    return this.conf;
  }

  @Override
  public List<String> getLabels(User user, Authorizations authorizations) {
    if (authorizations != null) {
      List<String> labels = authorizations.getLabels();
      String userName = user.getShortName();
      List<String> auths = this.labelsManager.getAuths(userName);
      return dropLabelsNotInUserAuths(labels, auths, userName);
    }
    return null;
  }

  private List<String> dropLabelsNotInUserAuths(List<String> labels, List<String> auths,
      String userName) {
    List<String> droppedLabels = new ArrayList<String>();
    List<String> passedLabels = new ArrayList<String>(labels.size());
    for (String label : labels) {
      if (auths.contains(label)) {
        passedLabels.add(label);
      } else {
        droppedLabels.add(label);
      }
    }
    if (!droppedLabels.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append("Dropping invalid authorizations requested by user ");
      sb.append(userName);
      sb.append(": [ ");
      for (String label: droppedLabels) {
        sb.append(label);
        sb.append(' ');
      }
      sb.append(']');
      LOG.warn(sb.toString());
    }
    return passedLabels;
  }
}
