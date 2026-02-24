package org.workflow.example;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.model.bpmn.builder.CamundaErrorEventDefinitionBuilder;

public class CamundaService {
    private final ProcessEngine processEngine;

    public CamundaService(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    public void startProcess(String processKey) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        runtimeService.startProcessInstanceByKey(processKey);
    }
}
