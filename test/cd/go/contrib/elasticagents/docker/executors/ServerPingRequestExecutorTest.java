/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cd.go.contrib.elasticagents.docker.executors;

import cd.go.contrib.elasticagents.Agent;
import cd.go.contrib.elasticagents.Agents;
import cd.go.contrib.elasticagents.docker.*;
import cd.go.contrib.elasticagents.docker.requests.CreateAgentRequest;
import cd.go.contrib.elasticagents.docker.requests.ServerPingRequest;
import org.joda.time.Period;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class ServerPingRequestExecutorTest extends BaseTest {

    @Test
    public void testShouldDisableIdleAgents() throws Exception {
        String agentId = UUID.randomUUID().toString();
        final Agents agents = new Agents(Arrays.asList(new Agent(agentId, "Idle", "Idle", "Enabled")));
        DockerContainers containers = new DockerContainers();

        PluginRequest pluginRequest = mock(PluginRequest.class);
        when(pluginRequest.getConfiguration()).thenReturn(createSettings());
        when(pluginRequest.getAgents()).thenReturn(agents);
        verifyNoMoreInteractions(pluginRequest);

        final Collection<Agent> values = agents.values();
        new ServerPingRequestExecutor(new ServerPingRequest(), containers, pluginRequest).execute();
        verify(pluginRequest).disable(argThat(collectionMatches(values)));

    }

    private ArgumentMatcher<Collection<Agent>> collectionMatches(final Collection<Agent> values) {
        return new ArgumentMatcher<Collection<Agent>>() {
            @Override
            public boolean matches(Object argument) {
                return new ArrayList<>((Collection) argument).equals(new ArrayList(values));
            }
        };
    }

    @Test
    public void testShouldTerminateDisabledAgents() throws Exception {
        String agentId = UUID.randomUUID().toString();
        final Agents agents = new Agents(Arrays.asList(new Agent(agentId, "Idle", "Idle", "Disabled")));
        DockerContainers containers = new DockerContainers();

        PluginRequest pluginRequest = mock(PluginRequest.class);
        when(pluginRequest.getConfiguration()).thenReturn(createSettings());
        when(pluginRequest.getAgents()).thenReturn(agents);
        verifyNoMoreInteractions(pluginRequest);

        new ServerPingRequestExecutor(new ServerPingRequest(), containers, pluginRequest).execute();
        final Collection<Agent> values = agents.values();
        verify(pluginRequest).delete(argThat(collectionMatches(values)));
    }

    @Test
    public void testShouldTerminateInstancesThatNeverAutoRegistered() throws Exception {
        PluginSettings settings = spy(createSettings());
        when(settings.getAutoRegisterPeriod()).thenReturn(new Period().withMinutes(0));

        PluginRequest pluginRequest = mock(PluginRequest.class);
        when(pluginRequest.getConfiguration()).thenReturn(settings);
        when(pluginRequest.getAgents()).thenReturn(new Agents());
        verifyNoMoreInteractions(pluginRequest);

        DockerContainers dockerContainers = new DockerContainers();
        DockerContainer container = dockerContainers.create(new CreateAgentRequest(null, null, null), settings);
        containers.add(container.id());

        ServerPingRequestExecutor serverPingRequestExecutor = new ServerPingRequestExecutor(new ServerPingRequest(), dockerContainers, pluginRequest);
        serverPingRequestExecutor.execute();

        assertFalse(dockerContainers.containsKey(container.id()));
    }
}
