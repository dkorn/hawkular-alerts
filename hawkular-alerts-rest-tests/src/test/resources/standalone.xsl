<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
    and other contributors as indicated by the @author tags.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:ds="urn:jboss:domain:datasources:3.0"
                xmlns:ra="urn:jboss:domain:resource-adapters:3.0"
                xmlns:ejb3="urn:jboss:domain:ejb3:3.0"
                xmlns:undertow="urn:jboss:domain:undertow:2.0"
                xmlns:tx="urn:jboss:domain:transactions:3.0"
                version="2.0"
                exclude-result-prefixes="xalan ds ra ejb3 undertow tx">

  <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
  <xsl:strip-space elements="*"/>

  <xsl:template match="node()[name(.)='cache-container'][1]">
    <xsl:copy>
      <xsl:copy-of select="node()|@*"/>
    </xsl:copy>
    <cache-container name="hawkular-alerts" default-cache="triggers" statistics-enabled="true">
      <local-cache name="partition"/>
      <local-cache name="triggers"/>
      <local-cache name="data"/>
      <local-cache name="publish"/>
      <local-cache name="dataIds" />
      <local-cache name="schema"/>
      <local-cache name="globalActions" />
    </cache-container>
  </xsl:template>

  <xsl:template match="node()[name(.)='periodic-rotating-file-handler']">
    <xsl:copy>
      <xsl:copy-of select="node()|@*"/>
    </xsl:copy>
    <logger category="org.hawkular.alerts">
      <level name="DEBUG"/>
    </logger>
  </xsl:template>

  <!-- add system properties -->
  <xsl:template name="system-properties">
    <system-properties>
      <property>
        <xsl:attribute name="name">hawkular.backend</xsl:attribute>
        <xsl:attribute name="value">&#36;{hawkular.backend:embedded_cassandra}</xsl:attribute>
      </property>
    </system-properties>
  </xsl:template>

  <!-- add additional subsystem extensions -->
  <xsl:template match="node()[name(.)='extensions']">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
    <xsl:call-template name="system-properties"/>
  </xsl:template>

  <!-- copy everything else as-is -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>