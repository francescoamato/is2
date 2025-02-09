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
package it.istat.is2.rule.domain;

import java.io.Serializable;
import javax.persistence.*;

import it.istat.is2.workflow.domain.Classification;
import lombok.Data;

/**
 * The persistent class for the SX_RULE database table.
 *
 */
@Data
@Entity
@Table(name = "SX_RULE")
@NamedQuery(name = "Rule.findAll", query = "SELECT s FROM Rule s")
public class Rule implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "ACTION")
    private String action;
    
    @Column(name = "code")
    private String code;

    private Short active;

    private Short blockrule;

    private String descr;

    private String eccezione;

    private Integer errcode;

    private String nome;

    private String rule;
    
    private Integer variabile;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "RTYPE", nullable = true)
    private RuleType ruleType;
    
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "CLASS", nullable = true)
    private Classification sxClassification;

    @ManyToOne
    @JoinColumn(name = "RULESET")
    private Ruleset ruleset;

    public Rule() {
    }
}
