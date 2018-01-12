/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import groovy.json.JsonSlurper

// script to read the Apache Sling repositories using the Github API
// it is recommended to use authentication by using the '-a' argument
// as otherwise requests will quickly become rate limited
//
// we currently support basic authentication with username and password
// or username and personal access token. Note that if you have
// two-factor authentication enabled you must use a personal access
// token

def parser = new JsonSlurper()

// get as many repos per page as possible to cut down on the number of calls
def link = "https://api.github.com/orgs/apache/repos?per_page=100"
def slingRepos = []
def groupsBlacklist = ['commons','resource', 'extensions']

def cli = new CliBuilder(usage: 'collect-sling-repos.groovy')
cli.a(args:1, 'Github authentication in username:password format (optional)')

def options = cli.parse(args)

while ( link ) {    
    def url = new URL(link)
    def conn = url.openConnection()
    if ( options.a ) {
        conn.setRequestProperty("Authorization", "Basic " + options.a.bytes.encodeBase64())
    }
    
    // add all projects matching naming conventions
    def result = parser.parse(conn.inputStream)    
    slingRepos += result
        .findAll { it.name.startsWith 'sling-' }

    // find link to next page, if applicable
    link = null
    
    def links = conn.headerFields['Link']
    if ( links ) {
        def next = links[0].split(',').find{ it.contains('rel="next"') }
        link = next != null ? next.find('<(.*)>').replaceAll('<|>',''): null
    }
}


// ensure a consistent order
slingRepos.sort {
    a,b -> a.name <=> b.name
}

println "# Apache Sling GitHub Repositories\n\n"
slingRepos.forEach {
    def m = it.name =~ 'sling-org-apache-sling-([a-z]+)-.*'
    if (m) {
        println " * [" + it.description + "]("+it.html_url+")"
    }
}
