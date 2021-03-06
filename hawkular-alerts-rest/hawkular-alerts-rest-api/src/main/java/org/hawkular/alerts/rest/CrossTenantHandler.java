/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.alerts.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import static org.hawkular.alerts.rest.HawkularAlertsApp.TENANT_HEADER_NAME;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.hawkular.alerts.api.model.event.Alert;
import org.hawkular.alerts.api.model.event.Event;
import org.hawkular.alerts.api.model.paging.Page;
import org.hawkular.alerts.api.model.paging.Pager;
import org.hawkular.alerts.api.services.AlertsCriteria;
import org.hawkular.alerts.api.services.AlertsService;
import org.hawkular.alerts.api.services.EventsCriteria;
import org.jboss.logging.Logger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST endpoint for cross tenant operations
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Path("/admin")
@Api(value = "/admin", description = "Cross tenant Operations")
public class CrossTenantHandler {
    private static final Logger log = Logger.getLogger(CrossTenantHandler.class);

    @HeaderParam(TENANT_HEADER_NAME)
    String tenantId;

    @EJB
    AlertsService alertsService;

    public CrossTenantHandler() {
        log.debug("Creating instance.");
    }

    @GET
    @Path("/alerts")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get alerts with optional filtering from multiple tenants.",
            notes = "If not criteria defined, it fetches all alerts available in the system. + \n" +
                    " + \n" +
                    "Multiple tenants are expected on HawkularTenant header as a comma separated list. + \n" +
                    "i.e. HawkularTenant: tenant1,tenant2,tenant3 + \n",
            response = Alert.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully fetched list of alerts."),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters", response = ResponseUtil.ApiError.class),
            @ApiResponse(code = 500, message = "Internal server error.", response = ResponseUtil.ApiError.class)
    })
    public Response findAlerts(
            @ApiParam(required = false, value = "Filter out alerts created before this time.",
                    allowableValues = "Timestamp in millisecond since epoch.")
            @QueryParam("startTime")
            final Long startTime,
            @ApiParam(required = false, value = "Filter out alerts created after this time.",
                    allowableValues = "Timestamp in millisecond since epoch.")
            @QueryParam("endTime")
            final Long endTime,
            @ApiParam(required = false, value = "Filter out alerts for unspecified alertIds.",
                    allowableValues = "Comma separated list of alert IDs.")
            @QueryParam("alertIds")
            final String alertIds,
            @ApiParam(required = false, value = "Filter out alerts for unspecified triggers. ",
                    allowableValues = "Comma separated list of trigger IDs.")
            @QueryParam("triggerIds")
            final String triggerIds,
            @ApiParam(required = false, value = "Filter out alerts for unspecified lifecycle status.",
                    allowableValues = "Comma separated list of [OPEN, ACKNOWLEDGED, RESOLVED]")
            @QueryParam("statuses")
            final String statuses,
            @ApiParam(required = false, value = "Filter out alerts for unspecified severity. ",
                    allowableValues = "Comma separated list of [LOW, MEDIUM, HIGH, CRITICAL]")
            @QueryParam("severities")
            final String severities,
            @ApiParam(required = false, value = "Filter out events for unspecified tags.",
                    allowableValues = "Comma separated list of tags, each tag of format 'name\\|value'. + \n" +
                            "Specify '*' for value to match all values.")
            @QueryParam("tags")
            final String tags,
            @ApiParam(required = false, value = "Filter out alerts resolved before this time.",
                    allowableValues = "Timestamp in millisecond since epoch.")
            @QueryParam("startResolvedTime")
            final Long startResolvedTime,
            @ApiParam(required = false, value = "Filter out alerts resolved after this time.",
                    allowableValues = "Timestamp in millisecond since epoch.")
            @QueryParam("endResolvedTime")
            final Long endResolvedTime,
            @ApiParam(required = false, value = "Filter out alerts acknowledged before this time.",
                    allowableValues = "Timestamp in millisecond since epoch.")
            @QueryParam("startAckTime")
            final Long startAckTime,
            @ApiParam(required = false, value = "Filter out alerts acknowledged after this time.",
                    allowableValues = "Timestamp in millisecond since epoch.")
            @QueryParam("endAckTime")
            final Long endAckTime,
            @ApiParam(required = false, value = "Return only thin alerts, do not include: evalSets, resolvedEvalSets.")
            @QueryParam("thin")
            final Boolean thin,
            @Context
            final UriInfo uri) {
        Pager pager = RequestUtil.extractPaging(uri);
        try {
            Set<String> tenantIds = getTenants(tenantId);
            AlertsCriteria criteria = new AlertsCriteria(startTime, endTime, alertIds, triggerIds, statuses,
                    severities, tags, startResolvedTime, endResolvedTime, startAckTime, endAckTime, thin);
            Page<Alert> alertPage = alertsService.getAlerts(tenantIds, criteria, pager);
            if (log.isDebugEnabled()) {
                log.debug("Alerts: " + alertPage);
            }
            if (isEmpty(alertPage)) {
                return ResponseUtil.ok(alertPage);
            }
            return ResponseUtil.paginatedOk(alertPage, uri);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    @GET
    @Path("/events")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get events with optional filtering from multiple tenants.",
            notes = "If not criteria defined, it fetches all events stored in the system. + \n" +
                    " + \n" +
                    "Multiple tenants are expected on HawkularTenant header as a comma separated list. + \n" +
                    "i.e. HawkularTenant: tenant1,tenant2,tenant3 + \n",
            response = Event.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully fetched list of events."),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters.", response = ResponseUtil.ApiError.class),
            @ApiResponse(code = 500, message = "Internal server error.", response = ResponseUtil.ApiError.class)
    })
    public Response findEvents(
            @ApiParam(required = false, value = "Filter out events created before this time.",
                    allowableValues = "Timestamp in millisecond since epoch.")
            @QueryParam("startTime")
            final Long startTime,
            @ApiParam(required = false, value = "Filter out events created after this time.",
                    allowableValues = "Timestamp in millisecond since epoch.")
            @QueryParam("endTime")
            final Long endTime,
            @ApiParam(required = false, value = "Filter out events for unspecified eventIds.",
                    allowableValues = "Comma separated list of event IDs.")
            @QueryParam("eventIds") final String eventIds,
            @ApiParam(required = false, value = "Filter out events for unspecified triggers.",
                    allowableValues = "Comma separated list of trigger IDs.")
            @QueryParam("triggerIds")
            final String triggerIds,
            @ApiParam(required = false, value = "Filter out events for unspecified categories. ",
                    allowableValues = "Comma separated list of category values.")
            @QueryParam("categories")
            final String categories,
            @ApiParam(required = false, value = "Filter out events for unspecified tags.",
                    allowableValues = "Comma separated list of tags, each tag of format 'name\\|value'. + \n" +
                            "Specify '*' for value to match all values.")
            @QueryParam("tags")
            final String tags,
            @ApiParam(required = false, value = "Return only thin events, do not include: evalSets.")
            @QueryParam("thin")
            final Boolean thin,
            @Context
            final UriInfo uri) {
        Pager pager = RequestUtil.extractPaging(uri);
        try {
            Set<String> tenantIds = getTenants(tenantId);
            EventsCriteria criteria = new EventsCriteria(startTime, endTime, eventIds, triggerIds, categories,
                    tags, thin);
            Page<Event> eventPage = alertsService.getEvents(tenantIds, criteria, pager);
            if (log.isDebugEnabled()) {
                log.debug("Events: " + eventPage);
            }
            if (isEmpty(eventPage)) {
                return ResponseUtil.ok(eventPage);
            }
            return ResponseUtil.paginatedOk(eventPage, uri);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    private Set<String> getTenants(String tenantId) {
        Set<String> tenantIds = new TreeSet<>();
        for (String t : tenantId.split(",")) {
            tenantIds.add(t);
        }
        return tenantIds;
    }

    private boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }
}
