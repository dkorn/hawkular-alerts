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
import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient
import net.sf.json.groovy.JsonSlurper

def url = "http://localhost:8080/hawkular/alerts/"
def tenant = "my-organization"

def definitions = new File("src/test/resources/autoresolve-process-definitions.json").text

RESTClient client = new RESTClient(url, ContentType.JSON)

client.handler.failure = { resp ->
    def mapper = new JsonSlurper()
    def error = mapper.parseText(resp.entity.content.text)
    println "Status error: ${resp.status} \nerrorMsg: [${error.errorMsg}]"
    return resp
}

if (System.getProperties().containsKey("hawkular")) {
    /*
        User: jdoe
        Password: password
        String encodedCredentials = Base64.getMimeEncoder()
        .encodeToString("$testUser:$testPasword".getBytes("utf-8"))
     */
    client.defaultRequestHeaders.Authorization = "Basic amRvZTpwYXNzd29yZA=="
} else {
    client.defaultRequestHeaders.put("Hawkular-Tenant", tenant)
}

def resp = client.post(path: "import/all", body: definitions)

if (resp.status == 200) {
    println "Imported definitions from json: \n${definitions}"
}