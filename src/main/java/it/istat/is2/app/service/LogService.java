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
package it.istat.is2.app.service;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.istat.is2.app.bean.SessionBean;
import it.istat.is2.app.dao.LogDao;
import it.istat.is2.app.domain.Log;
import it.istat.is2.app.util.IS2Const;
import java.util.ArrayList;

@Service
@Transactional
public class LogService {

    @Autowired
    private LogDao logDao;
    @Autowired
    private HttpSession httpSession;

    public List<Log> findAll() {
        return (List<Log>) this.logDao.findAll();
    }

    public List<Log> findByIdSessione(Long idSessione) {
       // return (List<Log>) this.logDao.findByIdSessioneAndTipoOrderByIdDesc(idSessione, IS2Const.OUTPUT_DEFAULT);
        return (List<Log>) this.logDao.findByIdSessioneOrderByIdAsc(idSessione);
    }

    public List<Log> findByIdSessioneAndTipo(Long idSessione, String tipo) {
        return (List<Log>) this.logDao.findByIdSessioneAndTipoOrderByIdAsc(idSessione, tipo);
    }
        
    public List<Log> findByIdSessione() {
         SessionBean sessionBean = (SessionBean) httpSession.getAttribute(IS2Const.SESSION_BEAN);
         List<Log> logs;
         if (sessionBean != null) {
             logs = this.logDao.findByIdSessioneAndTipoOrderByIdDesc(sessionBean.getId(),IS2Const.OUTPUT_DEFAULT);
         } else{
             logs = new ArrayList<>();
         }
        return logs;
    }
    
    public List<Log> findByIdSessioneAndTipo(String tipo) {
         SessionBean sessionBean = (SessionBean) httpSession.getAttribute(IS2Const.SESSION_BEAN);
         List<Log> logs;
         if (sessionBean != null) {
             logs = this.logDao.findByIdSessioneAndTipoOrderByIdAsc(sessionBean.getId(),tipo);
         } else{
             logs = new ArrayList<>();
         }
        return logs;
    }
    
    public int deleteByIdSessione(Long idSessione) {
        return this.logDao.deleteByIdSessione(idSessione);
    }
    
    public int deleteByIdSessioneAndTipo(Long idSessione, String tipo) {
        return this.logDao.deleteByIdSessioneAndTipo(idSessione, tipo);
    }
    
    public void save(String msg) {

        SessionBean sessionBean = (SessionBean) httpSession.getAttribute(IS2Const.SESSION_BEAN);

        Log log = new Log();
        if (sessionBean != null && sessionBean.getId()!=null ) {
            log.setIdSessione(sessionBean.getId());
        } else{
            log.setIdSessione(new Long(-1));
        }
        log.setMsg(msg);
        log.setTipo(IS2Const.OUTPUT_DEFAULT);
        log.setMsgTime(new Date());

        this.logDao.save(log);
    }
    
    public void save(String msg, String tipo) {

        SessionBean sessionBean = (SessionBean) httpSession.getAttribute(IS2Const.SESSION_BEAN);

        Log log = new Log();
        if (sessionBean != null) {
            log.setIdSessione(sessionBean.getId());
        } else{
            log.setIdSessione(new Long(-1));
        }
        log.setMsg(msg);
        log.setTipo(tipo);
        log.setMsgTime(new Date());

        this.logDao.save(log);
    }
    
    
}
