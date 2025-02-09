/**
 * Copyright 2019 ISTAT
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence. You may
 * obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * Licence for the specific language governing permissions and limitations under
 * the Licence.
 *
 * @author Francesco Amato <framato @ istat.it>
 * @author Mauro Bruno <mbruno @ istat.it>
 * @author Paolo Francescangeli  <pafrance @ istat.it>
 * @author Renzo Iannacone <iannacone @ istat.it>
 * @author Stefano Macone <macone @ istat.it>
 * @version 1.0
 */
package it.istat.is2.rule.controller;

import it.istat.is2.app.bean.InputFormBean;
import it.istat.is2.app.bean.NewRuleFormBean;
import it.istat.is2.app.bean.NewRulesetFormBean;
import it.istat.is2.app.bean.SessionBean;
import it.istat.is2.app.domain.Log;
import it.istat.is2.app.domain.User;
import it.istat.is2.app.service.LogService;
import it.istat.is2.app.service.NotificationService;
import it.istat.is2.app.util.FileHandler;
import it.istat.is2.app.util.IS2Const;
import static it.istat.is2.app.util.IS2Const.OUTPUT_R;

import it.istat.is2.dataset.domain.DatasetColonna;
import it.istat.is2.dataset.domain.DatasetFile;
import it.istat.is2.dataset.service.DatasetService;
import it.istat.is2.rule.domain.Rule;
import it.istat.is2.rule.domain.Ruleset;
import it.istat.is2.rule.forms.RuleCreateForm;
import it.istat.is2.rule.service.RuleService;
import it.istat.is2.workflow.domain.Classification;
import it.istat.is2.worksession.domain.WorkSession;
import it.istat.is2.worksession.service.WorkSessionService;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping("/rule")
@Controller
public class RuleController {

    @Autowired
    ServletContext context;
    @Autowired
    private MessageSource messages;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private WorkSessionService sessioneLavoroService;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private LogService logService;
    @Autowired
    private DatasetService datasetService;

    @RequestMapping(value = "/loadRulesFile", method = RequestMethod.POST)
    public String loadInputRulesData(HttpSession httpSession, HttpServletRequest request, Model model,
            @AuthenticationPrincipal User user, @ModelAttribute("inputFormBean") InputFormBean form)
            throws IOException {

        notificationService.removeAllMessages();
        // è il nome del file acquisito in automatico nel form di upload
        String nomeFile = form.getName();
        String descrizione = form.getDescrizione();
        // è il nome logico assegnato dall'utente ed è quello visualizzato nelle viste
        String etichetta = form.getLabelFile();
        String labelCodeRule = (!form.getLabelCodeRule().isEmpty()) ? form.getLabelCodeRule() : "R";
        String idclassificazione = form.getClassificazione();
        String separatore = form.getDelimiter();
        String idsessione = form.getIdsessione();
        Integer skipFirstLine = form.getSkipFirstLine();
        WorkSession sessionelv = sessioneLavoroService.getSessione(new Long(form.getIdsessione()));

        List<Ruleset> listaRS = ruleService.findRulesetBySessioneLavoro(sessionelv);
        boolean check = false;
        Iterator<Ruleset> itr = listaRS.iterator();
        while (itr.hasNext()) {
            Ruleset rs = itr.next();
            String label = rs.getLabelFile();
            // Controlla che la label assegnata dall'utente non sia già presente
            if (etichetta.equals(label)) {
                notificationService.addErrorMessage("Esiste già un Ruleset con quel nome. Specificare un nome diverso.");
                check = true;
                break;
            }
        }
        if (check == false) {
            File fileRules = FileHandler.convertMultipartFileToFile(form.getFileName());
            try {
                int rules = ruleService.loadRules(fileRules, idsessione, etichetta, labelCodeRule, idclassificazione,
                        separatore, nomeFile, descrizione, skipFirstLine);
                logService.save("Caricate " + rules + " regole");
                notificationService.addInfoMessage(
                        messages.getMessage("generic.save.success", null, LocaleContextHolder.getLocale()));
                SessionBean sessionBean = (SessionBean) httpSession.getAttribute(IS2Const.SESSION_BEAN);
                sessionBean.getRuleset().add(nomeFile);
                httpSession.setAttribute(IS2Const.SESSION_BEAN, sessionBean);
            } catch (Exception e) {
                notificationService.addErrorMessage(
                        messages.getMessage("rules.save.error", null, LocaleContextHolder.getLocale()), e.getMessage());
            }
        }

        return "redirect:/rule/viewRuleset/" + form.getIdsessione();
    }

    @RequestMapping(value = "/newRuleset", method = RequestMethod.POST)
    public String newRulesetData(HttpSession session, HttpServletRequest request, Model model,
            @AuthenticationPrincipal User user, @ModelAttribute("inputFormBean") NewRulesetFormBean form)
            throws IOException {

        notificationService.removeAllMessages();

        List<Log> logs = logService.findByIdSessione(Long.parseLong(form.getIdsessione()));
        List<Log> rlogs = logService.findByIdSessioneAndTipo(Long.parseLong(form.getIdsessione()), OUTPUT_R);

        List<Classification> listaClassificazioni;

        String nomeRuleset = form.getRulesetName();

        WorkSession sessionelv = sessioneLavoroService.getSessione(new Long(form.getIdsessione()));

        List<Ruleset> listaRS = ruleService.findRulesetBySessioneLavoro(sessionelv);
        boolean check = false;
        Iterator<Ruleset> itr = listaRS.iterator();
        while (itr.hasNext()) {
            Ruleset rs = itr.next();
            String label = rs.getLabelFile();
            // Controlla che la label assegnata dall'utente non sia già presente
            if (nomeRuleset.equals(label)) {
                notificationService.addErrorMessage("Esiste già un Ruleset con quel nome. Specificare un nome diverso.");
                check = true;
                break;
            }
        }
        if (check == false) {
            String descrRuleset = form.getRulesetDesc();
            Long dataset = form.getDataset();
            DatasetFile dfile = null;
            List<DatasetColonna> colonne = null;
            Ruleset ruleset = new Ruleset();

            if (dataset != -1) {
                dfile = datasetService.findDataSetFile(dataset);
                colonne = datasetService.findAllNomeColonne(dataset);
                ruleset.setDatasetFile(dfile);

                listaClassificazioni = new ArrayList<>();
                Classification onlyDominio = new Classification();
                onlyDominio.setId((short) 1);
                onlyDominio.setNome("Dominio");
                listaClassificazioni.add(onlyDominio);

                model.addAttribute("listaClassificazioni", listaClassificazioni);
            } else {

                listaClassificazioni = new ArrayList<>();
                Classification nullClass = new Classification();
                nullClass.setId((short) -1);
                nullClass.setNome("--");
                listaClassificazioni.add(nullClass);
                listaClassificazioni.addAll(ruleService.findAllClassifications());

                model.addAttribute("listaClassificazioni", listaClassificazioni);
            }

            ruleset.setLabelFile(nomeRuleset);
            ruleset.setDescr(descrRuleset);
            ruleset.setSessioneLavoro(sessionelv);

            model.addAttribute("colonne", colonne);
            SessionBean sessionBean = (SessionBean) session.getAttribute(IS2Const.SESSION_BEAN);

            sessionBean.setDataset(dataset);
            session.setAttribute(IS2Const.SESSION_BEAN, sessionBean);
            session.setAttribute("dfile", dfile);
            model.addAttribute("ruleset", ruleset);
            model.addAttribute("logs", logs);
            model.addAttribute("rlogs", rlogs);

            ruleService.saveRuleSet(ruleset);
        }

        return "redirect:/rule/viewRuleset/" + sessionelv.getId();
    }

    @RequestMapping(value = "/modificaRuleset", method = RequestMethod.POST)
    public String modificaRuleset(HttpSession session, HttpServletRequest request, Model model,
            @AuthenticationPrincipal User user, @ModelAttribute("inputFormBean") NewRulesetFormBean form)
            throws IOException {

        Ruleset ruleset = ruleService.findRulesetById(Integer.parseInt(form.getRulesetId()));
        String nomeRuleset = form.getRulesetName();
        WorkSession sessionelv = sessioneLavoroService.getSessione(new Long(form.getIdsessione()));

        List<Ruleset> listRS = ruleService.findAllRuleset();
        listRS.remove(ruleset);
        Iterator<Ruleset> itr = listRS.iterator();
        while (itr.hasNext()) {
            Ruleset rs = itr.next();
            String label = rs.getLabelFile();
            if (nomeRuleset.equals(label)) {
                notificationService.addErrorMessage("Esiste già un Ruleset con quel nome. Specificare un nome diverso.");
            } else {
                String descrRuleset = form.getRulesetDesc();
                Long datasetid = form.getDataset();
                DatasetFile ds = datasetService.findDataSetFile(datasetid);

                ruleset.setLabelFile(nomeRuleset);
                ruleset.setDescr(descrRuleset);
                ruleset.setDatasetFile(ds);

                ruleService.saveRuleSet(ruleset);
            }
        }

        return "redirect:/rule/viewRuleset/" + sessionelv.getId();

    }

    @RequestMapping(value = "/newRule", method = RequestMethod.POST)
    public String newRule(HttpSession session, HttpServletRequest request, Model model,
            @AuthenticationPrincipal User user, @ModelAttribute("inputFormBean") NewRuleFormBean form)
            throws IOException {

        notificationService.removeAllMessages();

        String nomeRule = form.getRuleName();

        String descrRule = form.getRuleDesc();
        Integer idRuleset = form.getIdruleset();
        String textRule = form.getRuleText();
        String textCode = form.getRuleCode();
        short idclassification = form.getClassification();
        Integer idVar = form.getIdcol();

        Ruleset ruleset = ruleService.findRulesetById(idRuleset);
        Rule sxrule = new Rule();

        Classification classification = ruleService.findClassificationById(idclassification);

        sxrule.setNome(nomeRule);
        sxrule.setDescr(descrRule);
        sxrule.setActive((short) 1);
        sxrule.setRule(textRule);
        sxrule.setCode(textCode);
        sxrule.setRuleset(ruleset);
        sxrule.setSxClassification(classification);
        sxrule.setVariabile(idVar);

        List<Rule> rules = ruleset.getRules();
        rules.add(sxrule);

        try {
            ruleService.saveRuleSet(ruleset);
            notificationService.addInfoMessage(messages.getMessage("generic.save.success", null, LocaleContextHolder.getLocale()));
        } catch (Exception e) {
            notificationService.addErrorMessage(messages.getMessage("rules.save.error", null, LocaleContextHolder.getLocale()), e.getMessage());
        }

        return "redirect:/rule/viewRules/" + ruleset.getId();

    }

    @PostMapping("/editRule")
    public String updateRule(@Valid @ModelAttribute("ruleCreateForm") RuleCreateForm form,
            BindingResult bindingResult) {

        notificationService.removeAllMessages();
        if (!bindingResult.hasErrors()) {

            try {
                ruleService.update(form);
                notificationService.addInfoMessage(messages.getMessage("rule.updated", null, LocaleContextHolder.getLocale()));
            } catch (Exception e) {
                notificationService.addErrorMessage("Error: " + e.getMessage());
            }
        } else {
            List<FieldError> errors = bindingResult.getFieldErrors();
            for (FieldError error : errors) {
                notificationService.addErrorMessage(error.getField() + " - " + error.getDefaultMessage());
            }
        }
        return "redirect:/rule/viewRules/" + form.getRuleSetId();
    }

    @GetMapping(value = "/viewRuleset/{id}")
    public String mostraruleset(HttpSession session, Model model, @PathVariable("id") Long id) {

        List<Log> logs = logService.findByIdSessione(id);
        List<Log> rlogs = logService.findByIdSessioneAndTipo(id, OUTPUT_R);

        WorkSession sessionelv = sessioneLavoroService.getSessione(id);
        List<DatasetFile> listaDSFile = new ArrayList<>();

        DatasetFile fakeFile = new DatasetFile();
        fakeFile.setId(new Long(-1));
        fakeFile.setNomeFile("--");
        listaDSFile.add(fakeFile);

        List<DatasetFile> listaDatasetFile = datasetService.findDatasetFilesByIdSessioneLavoro(id);

        listaDSFile.addAll(listaDatasetFile);

        List<Ruleset> listaRuleSet = sessionelv.getRuleSets();
        String etichetta;

        if (listaRuleSet != null && listaRuleSet.size() > 0) {
            String progressivo = Integer.toString(listaRuleSet.size() + 1);
            etichetta = "RS" + progressivo;

            List<Ruleset> listaRS = ruleService.findRulesetBySessioneLavoro(sessionelv);

            Iterator<Ruleset> itr = listaRS.iterator();
            while (itr.hasNext()) {
                Ruleset rs = itr.next();
                String label = rs.getLabelFile();
                // Controlla che la label assegnata dall'utente non sia già presente
                if (etichetta.equals(label)) {
                    int prog = Integer.parseInt(progressivo);
                    prog++;
                    etichetta = "RS" + prog;
                }
            }

        } else {
            etichetta = "RS1";
        }

        Ruleset rs;
        for (int i = 0; i < listaRuleSet.size(); i++) {
            rs = listaRuleSet.get(i);
            if (rs.getDatasetFile() == null) {
                rs.setNumeroRighe(rs.getRules().size());
            }
        }

        List<Classification> listaClassificazioni = ruleService.findAllClassifications();

        model.addAttribute("listaDatasetFile", listaDSFile);
        model.addAttribute("listaRuleSet", listaRuleSet);
        model.addAttribute("listaClassificazioni", listaClassificazioni);
        model.addAttribute("etichetta", etichetta);
        model.addAttribute("logs", logs);
        model.addAttribute("rlogs", rlogs);

        return "ruleset/list";
    }

    @RequestMapping("/viewRules/{idfile}")
    public String viewRules(HttpSession session, Model model, @PathVariable("idfile") Integer idfile) {

        session.removeAttribute("dfile");

        List<Log> logs = logService.findByIdSessione();
        List<Log> rlogs = logService.findByIdSessioneAndTipo(OUTPUT_R);
        // TODO: Controllare findRulesetById
        Ruleset ruleset = ruleService.findRulesetById(idfile);
        List<Rule> rules = ruleService.findRules(ruleset);

        DatasetFile dfile = ruleset.getDatasetFile();

        model.addAttribute("ruleset", ruleset);
        model.addAttribute("rules", rules);

        List<Classification> listaClassificazioni;
        if (dfile != null) {
            listaClassificazioni = new ArrayList<>();
            Classification onlyDominio = new Classification();
            onlyDominio.setId((short) 1);
            onlyDominio.setNome("Dominio");
            listaClassificazioni.add(onlyDominio);
            model.addAttribute("listaClassificazioni", listaClassificazioni);
        } else {
            listaClassificazioni = ruleService.findAllClassifications();
            model.addAttribute("listaClassificazioni", listaClassificazioni);
        }
        session.setAttribute("dfile", dfile);
        model.addAttribute("logs", logs);
        model.addAttribute("rlogs", rlogs);

        return "ruleset/preview";
    }

    @GetMapping("/runvalidate/{idRuleset}")
    public String runValidate(HttpSession session, Model model, @PathVariable("idRuleset") Integer idRuleset) {
        notificationService.removeAllMessages();
        Ruleset ruleset = ruleService.findRulesetById(idRuleset);
        try {
            Map<String, List<String>> resulVal = ruleService.runValidate(ruleset);
            for (Entry<String, List<String>> me : resulVal.entrySet()) {
                model.addAttribute(me.getKey().toString(), me.getValue());
            }
        } catch (Exception e) {
            //notificationService.addErrorMessage("Error: " + e.getMessage());
        }
        List<Log> logs = logService.findByIdSessione();
        List<Log> rlogs = logService.findByIdSessioneAndTipo(OUTPUT_R);
        // TODO: Controllare findRulesetById

        List<Rule> rules = ruleService.findRules(ruleset);

        DatasetFile dfile = ruleset.getDatasetFile();

        model.addAttribute("ruleset", ruleset);
        model.addAttribute("rules", rules);

        List<Classification> listaClassificazioni;
        if (dfile != null) {
            listaClassificazioni = new ArrayList<>();
            Classification onlyDominio = new Classification();
            onlyDominio.setId((short) 1);
            onlyDominio.setNome("Dominio");
            listaClassificazioni.add(onlyDominio);
            model.addAttribute("listaClassificazioni", listaClassificazioni);
        } else {
            listaClassificazioni = ruleService.findAllClassifications();
            model.addAttribute("listaClassificazioni", listaClassificazioni);
        }
        session.setAttribute("dfile", dfile);
        model.addAttribute("logs", logs);
        model.addAttribute("rlogs", rlogs);

        return "ruleset/preview";

    }

    @GetMapping(value = "/deleteRuleset/{idSessione}/{idRuleset}")
    public String eliminaRuleset(HttpSession session, Model model, @PathVariable("idSessione") Long idSessione,
            @PathVariable("idRuleset") Integer idRuleset) {

        notificationService.removeAllMessages();

        ruleService.deleteRuleset(idRuleset);

        logService.save("Set di regole con id " + idRuleset + " eliminato con successo");
        notificationService.addInfoMessage("Eliminazione avvenuta con successo");

        return "redirect:/rule/viewRuleset/" + idSessione;
    }
}
