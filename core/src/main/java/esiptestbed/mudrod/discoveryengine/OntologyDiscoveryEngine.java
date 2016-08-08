/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you 
 * may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package esiptestbed.mudrod.discoveryengine;

import java.util.Map;

import esiptestbed.mudrod.driver.ESDriver;
import esiptestbed.mudrod.driver.SparkDriver;
import esiptestbed.mudrod.ontology.pre.AggregateTriples;
import esiptestbed.mudrod.ontology.process.OntologyLinkCal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supports to preprocess and process ontology
 */
public class OntologyDiscoveryEngine extends DiscoveryEngineAbstract {
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(OntologyDiscoveryEngine.class);

  /**
   * Constructor supporting a number of parameters documented below.
   * @param config a {@link java.util.Map} containing K,V of type String, String respectively.
   * @param es the {@link esiptestbed.mudrod.driver.ESDriver} used to persist log files.
   * @param spark the {@link esiptestbed.mudrod.driver.SparkDriver} used to process input log files.
   */
  public OntologyDiscoveryEngine(Map<String, String> config, ESDriver es,
      SparkDriver spark) {
    super(config, es, spark);
  }

  /**
   * Method of preprocessing ontology
   */
  public void preprocess() {
    LOG.info("*****************Ontology preprocessing starts******************");
    startTime = System.currentTimeMillis();

    DiscoveryStepAbstract at = new AggregateTriples(this.config, this.es,
        this.spark);
    at.execute();

    endTime = System.currentTimeMillis();
    LOG.info("*****************Ontology preprocessing ends******************Took {}s",
        (endTime - startTime) / 1000);
  }

  /**
   * Method of processing ontology
   */
  public void process() {
    LOG.info("*****************Ontology processing starts******************");
    startTime = System.currentTimeMillis();

    DiscoveryStepAbstract ol = new OntologyLinkCal(this.config, this.es,
        this.spark);
    ol.execute();

    endTime = System.currentTimeMillis();
    LOG.info("*****************Ontology processing ends******************Took {}s", (endTime - startTime) / 1000);
  }

  public void output() {
  }

}
