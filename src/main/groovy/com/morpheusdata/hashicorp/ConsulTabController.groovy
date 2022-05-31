package com.morpheusdata.consul


import com.morpheusdata.model.Permission
import com.morpheusdata.views.JsonResponse
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.ViewModel
import com.morpheusdata.web.PluginController
import com.morpheusdata.web.Route
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.MorpheusContext
import groovy.json.JsonOutput
import groovy.json.JsonSlurper


class ConsulTabController implements PluginController {

	MorpheusContext morpheusContext
	Plugin plugin

	public ConsulTabController(Plugin plugin, MorpheusContext morpheusContext) {
		this.plugin = plugin
		this.morpheusContext = morpheusContext
	}

	@Override
	public String getCode() {
		return 'consulTabController'
	}

	@Override
	String getName() {
		return 'HashiCorp Consul Tab Controller'
	}

	@Override
	MorpheusContext getMorpheus() {
		return morpheusContext
	}

	List<Route> getRoutes() {
		[
			Route.build("/customTab/json", "json", Permission.build("admin", "full"))
		]
	}

	def json(ViewModel<Map> model) {
		println model
		model.object.foo = "fizz"
		return JsonResponse.of(model.object)
	}
}