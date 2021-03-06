/**
 * =============================================================================
 *
 * ORCID (R) Open Source
 * http://orcid.org
 *
 * Copyright (c) 2012-2014 ORCID, Inc.
 * Licensed under an MIT-Style License (MIT)
 * http://orcid.org/open-source-license
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 *
 * =============================================================================
 */
package org.orcid.core.manager.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import org.apache.commons.lang3.tuple.Pair;
import org.orcid.core.manager.OrgDisambiguatedManager;
import org.orcid.persistence.constants.OrganizationStatus;
import org.orcid.persistence.dao.OrgDisambiguatedDao;
import org.orcid.persistence.dao.OrgDisambiguatedSolrDao;
import org.orcid.persistence.jpa.entities.IndexingStatus;
import org.orcid.persistence.jpa.entities.OrgDisambiguatedEntity;
import org.orcid.persistence.jpa.entities.OrgDisambiguatedExternalIdentifierEntity;
import org.orcid.persistence.jpa.entities.OrgEntity;
import org.orcid.pojo.OrgDisambiguated;
import org.orcid.pojo.OrgDisambiguatedExternalIdentifiers;
import org.orcid.utils.solr.entities.OrgDisambiguatedSolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 
 * @author Will Simpson
 * 
 */
public class OrgDisambiguatedManagerImpl implements OrgDisambiguatedManager {

    private static final int INDEXING_CHUNK_SIZE = 1000;
    private static final int INCORRECT_POPULARITY_CHUNK_SIZE = 1000;
    private static final String FUNDING_ORG_TYPE = "FUNDREF";
    private static final Logger LOGGER = LoggerFactory.getLogger(OrgDisambiguatedManagerImpl.class);

    @Resource
    private OrgDisambiguatedDao orgDisambiguatedDao;

    @Resource
    private OrgDisambiguatedDao orgDisambiguatedDaoReadOnly;
    
    @Resource
    private OrgDisambiguatedSolrDao orgDisambiguatedSolrDao;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    synchronized public void processOrgsForIndexing() {
        LOGGER.info("About to process disambiguated orgs for indexing");
        List<OrgDisambiguatedEntity> entities = null;
        do {
            entities = orgDisambiguatedDaoReadOnly.findOrgsByIndexingStatus(IndexingStatus.PENDING, 0, INDEXING_CHUNK_SIZE);
            LOGGER.info("Found chunk of {} disambiguated orgs for indexing", entities.size());
            for (OrgDisambiguatedEntity entity : entities) {
                processDisambiguatedOrgInTransaction(entity);
            }
        } while (!entities.isEmpty());

    }

    private void processDisambiguatedOrgInTransaction(final OrgDisambiguatedEntity entity) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                processDisambiguatedOrg(orgDisambiguatedDaoReadOnly.find(entity.getId()));
            }
        });
    }

    private void processDisambiguatedOrg(OrgDisambiguatedEntity entity) {
        LOGGER.info("About to index disambiguated org, id={}", entity.getId());
        OrgDisambiguatedSolrDocument document = convertEntityToDocument(entity);
        if(OrganizationStatus.DEPRECATED.name().equals(entity.getStatus()) || OrganizationStatus.OBSOLETE.name().equals(entity.getStatus())) {
            orgDisambiguatedSolrDao.remove(document.getOrgDisambiguatedId());            
        } else {
            orgDisambiguatedSolrDao.persist(document);
        }
        
        orgDisambiguatedDao.updateIndexingStatus(entity.getId(), IndexingStatus.DONE);
    }

    private OrgDisambiguatedSolrDocument convertEntityToDocument(OrgDisambiguatedEntity entity) {
        OrgDisambiguatedSolrDocument document = new OrgDisambiguatedSolrDocument();
        document.setOrgDisambiguatedId(entity.getId());
        document.setOrgDisambiguatedName(entity.getName());
        document.setOrgDisambiguatedCity(entity.getCity());
        document.setOrgDisambiguatedRegion(entity.getRegion());
        if (entity.getCountry() != null)
            document.setOrgDisambiguatedCountry(entity.getCountry().value());
        document.setOrgDisambiguatedType(entity.getOrgType());
        document.setOrgDisambiguatedPopularity(entity.getPopularity());
        Set<String> orgNames = new HashSet<>();
        orgNames.add(entity.getName());
        Set<OrgEntity> orgs = entity.getOrgs();
        if (orgs != null) {
            for (OrgEntity org : orgs) {
                orgNames.add(org.getName());
            }
        }
        document.setOrgNames(new ArrayList<>(orgNames));

        if (FUNDING_ORG_TYPE.equals(entity.getSourceType())) {
            document.setFundingOrg(true);
        } else {
            document.setFundingOrg(false);
        }

        return document;
    }

    @Override
    synchronized public void processOrgsWithIncorrectPopularity() {
        LOGGER.info("About to process disambiguated orgs with incorrect popularity");
        List<Pair<Long, Integer>> pairs = null;
        do {
            pairs = orgDisambiguatedDaoReadOnly.findDisambuguatedOrgsWithIncorrectPopularity(INCORRECT_POPULARITY_CHUNK_SIZE);
            LOGGER.info("Found chunk of {} disambiguated orgs with incorrect popularity", pairs.size());
            for (Pair<Long, Integer> pair : pairs) {
                LOGGER.info("About to update popularity of disambiguated org: {}", pair);
                orgDisambiguatedDao.updatePopularity(pair.getLeft(), pair.getRight());
            }
        } while (!pairs.isEmpty());
    }

    @Override
    public List<OrgDisambiguated> searchOrgsFromSolr(String searchTerm, int firstResult, int maxResult, boolean fundersOnly) {
        List<OrgDisambiguatedSolrDocument> docs = orgDisambiguatedSolrDao.getOrgs(searchTerm, firstResult, maxResult, fundersOnly);
        List<OrgDisambiguated> ret = new ArrayList<OrgDisambiguated>();
        for (OrgDisambiguatedSolrDocument doc: docs){
            OrgDisambiguated org = new OrgDisambiguated();
            org.setValue(doc.getOrgDisambiguatedName());
            org.setCity(doc.getOrgDisambiguatedCity());
            org.setRegion(doc.getOrgDisambiguatedRegion());
            org.setCountry(doc.getOrgDisambiguatedCountry());
            org.setOrgType(doc.getOrgDisambiguatedType());
            org.setDisambiguatedAffiliationIdentifier(Long.toString(doc.getOrgDisambiguatedId()));
            ret.add(org);            
        }
        return ret;
    }

    @Override
    @Transactional
    public OrgDisambiguated findInDB(Long id) {
        OrgDisambiguatedEntity orgDisambiguatedEntity = orgDisambiguatedDaoReadOnly.find(id);
        OrgDisambiguated org = new OrgDisambiguated();
        org.setValue(orgDisambiguatedEntity.getName());
        org.setCity(orgDisambiguatedEntity.getCity());
        org.setRegion(orgDisambiguatedEntity.getRegion());        
        org.setCountry(orgDisambiguatedEntity.getCountry() != null ? orgDisambiguatedEntity.getCountry().value() : null);
        org.setOrgType(orgDisambiguatedEntity.getOrgType());
        org.setSourceId(orgDisambiguatedEntity.getSourceId());
        org.setSourceType(orgDisambiguatedEntity.getSourceType()); 
        org.setUrl(orgDisambiguatedEntity.getUrl());
        Map<String, OrgDisambiguatedExternalIdentifiers> externalIdsMap = new HashMap<String, OrgDisambiguatedExternalIdentifiers>();
        if (orgDisambiguatedEntity.getExternalIdentifiers() != null && !orgDisambiguatedEntity.getExternalIdentifiers().isEmpty()) {
            for (OrgDisambiguatedExternalIdentifierEntity extIdEntity : orgDisambiguatedEntity.getExternalIdentifiers()) {
                String type = extIdEntity.getIdentifierType();
                String identifier = extIdEntity.getIdentifier();
                Boolean preferred = extIdEntity.getPreferred();

                OrgDisambiguatedExternalIdentifiers extId = null;

                if (externalIdsMap.containsKey(type)) {
                    extId = externalIdsMap.get(type);
                } else {
                    extId = new OrgDisambiguatedExternalIdentifiers();
                    extId.setIdentifierType(type);
                    externalIdsMap.put(type, extId);
                }

                if (preferred) {
                    extId.setPreferred(identifier);
                }

                extId.getAll().add(identifier);
            }

            if (!externalIdsMap.isEmpty()) {
                List<OrgDisambiguatedExternalIdentifiers> extIds = new ArrayList<OrgDisambiguatedExternalIdentifiers>();
                externalIdsMap.keySet().stream().sorted().collect(Collectors.toList()).forEach(k -> {extIds.add(externalIdsMap.get(k));});                
                org.setOrgDisambiguatedExternalIdentifiers(extIds);
            }

        }
        return org;
    }

}
