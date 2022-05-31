package com.morpheusdata.consul

import com.morpheusdata.core.AbstractInstanceTabProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.Account
import com.morpheusdata.model.Instance
import com.morpheusdata.model.TaskConfig
import com.morpheusdata.model.ContentSecurityPolicy
import com.morpheusdata.model.User
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.ViewModel
import com.morpheusdata.core.util.RestApiUtil
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.reactivex.Single
import groovy.util.logging.Slf4j

@Slf4j
class ConsulTabProvider extends AbstractInstanceTabProvider {
	Plugin plugin
	MorpheusContext morpheus
	RestApiUtil consulAPI

	String code = 'hashicorp-consul-tab'
	String name = 'HashiCorp Consul'

	ConsulTabProvider(Plugin plugin, MorpheusContext context) {
		this.plugin = plugin
		this.morpheus = context
		this.consulAPI = new RestApiUtil()
	}

	ConsulTabProvider(Plugin plugin, MorpheusContext morpheusContext, RestApiUtil api) {
		this.morpheusContext = morpheusContext
		this.plugin = plugin
		this.consulAPI = api
	}

	@Override
	HTMLResponse renderTemplate(Instance instance) {
		ViewModel<Instance> model = new ViewModel<>()
		TaskConfig config = morpheus.buildInstanceConfig(instance, [:], null, [], [:]).blockingGet()
		try {
			// Retrieve plugin settings
			def settings = morpheus.getSettings(plugin)
			def settingsOutput = ""
			settings.subscribe(
				{ outData -> 
					settingsOutput = outData
				},
				{ error ->
					println error.printStackTrace()
				}
			)

			// Parse the plugin settings payload. The settings will be available as
			// settingsJson.$optionTypeFieldName i.e. - settingsJson.ddApiKey to retrieve the DataDog API key setting
			JsonSlurper slurper = new JsonSlurper()
			def settingsJson = slurper.parseText(settingsOutput)
			def HashMap<String, String> consulPayload = new HashMap<String, String>();
			consulPayload.put("name", instance.name)
			def results = consulAPI.callApi(settingsJson.consulServerUrl, "v1/health/node/${instance.name}", "", "", new RestApiUtil.RestOptions(headers:['Content-Type':'application/json'], ignoreSSL: false), 'GET')
			def json = slurper.parseText(results.content)

			if (json.size == 0){
				getRenderer().renderTemplate("hbs/instanceNotFoundTab", model)
			} else {
				def status = json[0].Status
				consulPayload.put("status", status)
				def nodeServices = consulAPI.callApi(settingsJson.consulServerUrl, "v1/catalog/node-services/${instance.name}", "", "", new RestApiUtil.RestOptions(headers:['Content-Type':'application/json'], ignoreSSL: false), 'GET')
				def servicesJson = slurper.parseText(nodeServices.content)
				def services = servicesJson
				if (services.Services == "null"){
					consulPayload.put("servicesCount",0)
				} else {
					consulPayload.put("servicesCount", services.Services.size)
				}
				consulPayload.put("datacenter", services.Node.Datacenter)
				consulPayload.put("services", services.Services)
				consulPayload.put("metadata", services.Node.Meta)
	    		model.object = consulPayload
				getRenderer().renderTemplate("hbs/instanceTab", model)
			}
		}
		catch(Exception ex) {
          	println "Error parsing the Consul plugin settings. Ensure that the plugin settings have been configured."
		  	getRenderer().renderTemplate("hbs/instanceNotFoundTab", model)
        }
	
	}

	@Override
	Boolean show(Instance instance, User user, Account account) {
		def show = true
		println "user has permissions: ${user.permissions}"
		println "instanceType ${instance.instanceTypeCode}"
		println "provisionType ${instance.provisionType}"
		// plugin.permissions.each { Permission permission ->
		// 	if(user.permissions[permission.code] != permission.availableAccessTypes.last().toString()){
		// 		show = false
		// 	}
		// }
		return show
	}

	@Override
	ContentSecurityPolicy getContentSecurityPolicy() {
		def csp = new ContentSecurityPolicy()
		csp
	}
}