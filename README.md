# CloudBees Docker Traceability Plugin

This [Jenkins](http://jenkins-ci.org) plugin allows the tracking of the creation and use of Docker containers in Jenkins and their future use.

#Plugin Summary

* Container deployments summary page
* Tracking of Docker image and container deployments produced in Jenkins
* Tracking of Docker container events
* Tracking of Docker container states being retrieved from *docker inspect* calls
* Advanced [API](#API):
 * Submission of events from Docker and Docker Swarm installations
 * Data polling from Jenkins (including Docker API-compatible JSONs)
 * Support of search queries
 
 ![Main page](/doc/images/root-action.png)

#Installation Guidelines

##Plugin setup

1. Install Docker Traceability plugin from Jenkins Update Center
2. If you are going to use Docker Traceability for images built on Jenkins, consider the installation of other
3. Configure the security. This step is **very important**, because the plugin can store raw JSON, which may contain sensitive info like passwords
 * The plugin introduces new permissions, which allow to restrict the access
 * **Read** - Allows to retrieve details like full container/info dumps. 
      Web interfaces are being managed by common Jenkins Read permission
 * **Submit** - Allows to submit deployment records from [API](#API)
 * **Delete** - Allows to delete deployment records or entire fingerprints

Currently, the plugin does not require additional configuration.

##Client-side configuration

```
The section is under construction
```

#Use-cases

##Submitting deployment records

Currently the plugin does not support an automatic polling of events from external Docker servers. The events should be submitted by external clients or other Jenkins plugins.

**Warning!** Currently the plugin accepts the info for previously registered fingerprints only. Other submissions will be ignored. Initial image records should be created by other plugins using (see [Integrations][#Integrations])

From external items using REST API
-----
The [plugin's API](#API) provides several commands, which allow to submit new events from remote applications.

**submitContainerStatus** is a simple call, which may be used from user scripts without a generation of additional JSON files.
 
Examples: 
```
 curl http://localhost:8080/jenkins/docker-traceability/submitContainerStatus 
    --data-urlencode inspectData="$(docker inspect CONTAINER_ID)"
 
curl http://localhost:8080/jenkins/docker-traceability/submitContainerStatus 
    --data-urlencode status=create
    --data-urlencode imageName=jenkinsci/workflow-demo
    --data-urlencode hostName=dev-server-1
    --data-urlencode hostName=development
    --data-urlencode inspectData="$(docker inspect CONTAINER_ID)"
```

**submitEvent** is a more complex call, which allows submit the all available data about a Docker container via a single REST API call. This call can be used from external plugins.

From other plugins
-----
The plugin provides the <code>DockerEventListener</code> extension point, which is being used to notify listeners about new records. 

Docker Traceability functionality also listens to these endpoints, so it is possible to notify the plugin about new records using <code>DockerEventListener#fire()</code> method.

## Getting info from the plugin

For each container record the plugin publishes the info on the container summary page. A summary status is being also added to the parent image page.

![Docker container page](/doc/images/container-page.png)

![Docker image page](/doc/images/image-page.png)


If an external client submits an info about the image (can be retrieved using *docker inspect imageId* command), Docker Traceability Plugin captures this image and adds a new facet to the image fingerprint page.

![Docker image info facet](/doc/images/docker-image-facet.png)

Raw data is accessible via the [plugin's API](#API) or via hyperlinks on info pages.

## Search

You can search deployments by container IDs using the "Search" control on the "Docker Traceability" page. You can also query containers using the [plugin's API](#API).

#Integrations

CloudBees Docker Traceability plugin is based on fingerprints provided by [Docker Commons Plugin][docker-commons]. The plugin just adds additional  facets to main fingerprint pages, so any other plugin can contribute to the UI by adding additional facets to the fingerprint. 

See [Docker Commons Plugin Wiki page][docker-commons] to get an info about existing fingerprint contributors.

#API

The detailed description of API endpoints is available in the "api" page of the Docker Traceability root action (see *$(JENKINS_URL)/docker-traceability/api*)

[docker-commons]: https://wiki.jenkins-ci.org/display/JENKINS/Docker+Commons+Plugin