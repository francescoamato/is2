package it.istat.is2.workflow.batch;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import it.istat.is2.app.service.LogService;
import it.istat.is2.app.service.NotificationService;
import it.istat.is2.workflow.dao.BusinessProcessDao;
import it.istat.is2.workflow.domain.Elaborazione;
import it.istat.is2.workflow.domain.BusinessProcess;
import it.istat.is2.workflow.domain.BusinessStep;
import it.istat.is2.workflow.domain.StepInstance;
import it.istat.is2.workflow.engine.EngineFactory;
import it.istat.is2.workflow.engine.EngineService;
import it.istat.is2.workflow.service.WorkflowService;

@Component
@StepScope
public class WorkFlowBatchProcessor implements ItemReader<Elaborazione> {

    @Value("#{jobParameters['idElaborazione']}")
    private Long idElaborazione;

    @Value("#{jobParameters['idBProc']}")
    private Long idBProc;

    @Autowired
    BusinessProcessDao businessProcessDao;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    EngineFactory engineFactory;

    @Autowired
    private LogService logService;
    @Autowired
	private NotificationService notificationService;

    @Override
    public Elaborazione read()
            throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        Elaborazione elaborazione = workflowService.findElaborazione(idElaborazione);
        BusinessProcess businessProcess = businessProcessDao.findById(idBProc).orElse(new BusinessProcess());
        for (Iterator<?> iterator = businessProcess.getBusinessSteps().iterator(); iterator.hasNext();) {
            BusinessStep businessStep = (BusinessStep) iterator.next();
            for (Iterator<?> iteratorStep = businessStep.getStepInstances().iterator(); iteratorStep.hasNext();) {
                StepInstance stepInstance = (StepInstance) iteratorStep.next();
                elaborazione = doStep(elaborazione, stepInstance);
            }
        }
        return null;
    }

    public Elaborazione doStep(Elaborazione elaborazione, StepInstance stepInstance) throws Exception {
        EngineService engine = engineFactory.getEngine(stepInstance.getAppService().getInterfaccia());
       
        try {
            engine.init(elaborazione, stepInstance);
            engine.doAction();
            engine.processOutput();
        } catch (Exception e) {
         	 Logger.getRootLogger().error(e.getMessage());
             notificationService.addErrorMessage("Error: " + e.getMessage());
             throw (e);
        } finally {
            engine.destroy();
        }

        return elaborazione;
    }

    public void setIdElaborazione(Long idElaborazione) {
        this.idElaborazione = idElaborazione;
    }

    public void setIdBProc(Long idBProc) {
        this.idBProc = idBProc;
    }

}
