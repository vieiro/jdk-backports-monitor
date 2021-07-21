/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.backports.report.model;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.common.collect.*;
import org.openjdk.backports.jira.Accessors;
import org.openjdk.backports.jira.UserCache;

import java.io.PrintStream;
import java.util.*;

public class PushesModel extends AbstractModel {

    private final boolean directOnly;
    private final String release;
    private final List<Issue> issues;
    private final Multiset<String> byPriority;
    private final Multiset<String> byComponent;
    private final Multimap<String, Issue> byCommitter;
    private final Set<Issue> byTime;
    private final SortedSet<Issue> noChangesets;

    public PushesModel(JiraRestClient restClient, PrintStream debugOut, boolean directOnly, String release) {
        super(restClient, debugOut);
        this.directOnly = directOnly;
        this.release = release;
        this.issues = jiraIssues.getIssues("project = JDK" +
                        " AND (status in (Closed, Resolved))" +
                        " AND (resolution not in (\"Won't Fix\", Duplicate, \"Cannot Reproduce\", \"Not an Issue\", Withdrawn, Other))" +
                        (directOnly ? " AND type != Backport" : "") +
                        " AND (issuetype != CSR)" +
                        " AND fixVersion = " + release,
                false);

        Comparator<Issue> chronologicalCompare = Comparator.comparing(Accessors::getPushSecondsAgo).thenComparing(Comparator.comparing(Issue::getKey).reversed());

        byPriority = TreeMultiset.create();
        byComponent = HashMultiset.create();
        byCommitter = TreeMultimap.create(String::compareTo, DEFAULT_ISSUE_SORT);
        byTime = new TreeSet<>(chronologicalCompare);

        noChangesets = new TreeSet<>(chronologicalCompare);

        for (Issue issue : issues) {
            String committer = Accessors.getPushUser(issue);
            if (committer.equals("N/A")) {
                // These are pushes to internal repos
                noChangesets.add(issue);
            } else {
                byPriority.add(issue.getPriority().getName());
                byComponent.add(Accessors.extractComponents(issue));
                byCommitter.put(committer, issue);
                byTime.add(issue);
            }
        }

        debugOut.println("Filtered " + noChangesets.size() + " issues without pushes, " + byPriority.size() + " pushes left.");
    }

    public String release() {
        return release;
    }

    public boolean directOnly() {
        return directOnly;
    }

    public UserCache users() {
        return users;
    }

    public List<Issue> issues() {
        return issues;
    }

    public Multiset<String> byPriority() {
        return byPriority;
    }

    public Multiset<String> byComponent() {
        return byComponent;
    }

    public Multimap<String, Issue> byCommitter() {
        return byCommitter;
    }

    public Set<Issue> byTime() {
        return byTime;
    }

    public SortedSet<Issue> noChangesets() {
        return noChangesets;
    }
}
