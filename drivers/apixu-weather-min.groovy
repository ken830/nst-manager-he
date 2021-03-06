/***********************************************************************************************************************
*  Copyright 2018 bangali
*
*  Contributors:
*	https://github.com/jebbett	code for new weather icons based on weather condition data
*	https://www.deviantart.com/vclouds/art/VClouds-Weather-Icons-179152045	 new weather icons courtesy of VClouds
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*	http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*  ApiXU Weather Driver
*
*  Author: bangali
*
***********************************************************************************************************************/

public static String version()	{ return "v1.0.4" }

/***********************************************************************************************************************
*
* Based off Version: 4.2.0
*/

import groovy.transform.Field

metadata	{
	definition (name: "ApiXU Weather Driver Min", namespace: "bangali", author: "bangali", importUrl: "https://raw.githubusercontent.com/tonesto7/nst-manager-he/master/drivers/apixu-weather-min.groovy") {
		capability "Initialize"
//		capability "Actuator"
		capability "Sensor"
		capability "Polling"
		capability "Illuminance Measurement"
		capability "Temperature Measurement"
		capability "Relative Humidity Measurement"
		capability "Pressure Measurement"
		capability "Ultraviolet Index"
//		capability "Switch"

// Standard attributes from capabilities
//		attribute "temperature", "string"
//		attribute "humidity", "string"
//		attribute "illuminance", "string"
//		attribute "pressure", "string"
//		attribute "ultravioletIndex", "string"


// name not needed  city has this
//		attribute "name", "string"

		attribute "region", "string"
		attribute "country", "string"

// not needed
//		attribute "lat", "string"
//		attribute "lon", "string"
//		attribute "tz_id", "string"

// these only used in dashboards
//		attribute "localtime_epoch", "string"
//		attribute "local_time", "string"
//		attribute "local_date", "string"
//		attribute "last_updated_epoch", "string"

		attribute "last_updated", "string"

// not needed
//		attribute "temp_c", "string"
//		attribute "temp_f", "string"
// not needed
//		attribute "is_day", "string"
//		attribute "condition_text", "string"
//		attribute "condition_icon", "string"
//		attribute "condition_icon_url", "string"
//		attribute "condition_icon_only", "string"
		attribute "condition_code", "string"
//		attribute "visual", "string"
//		attribute "visualWithText", "string"

// obsolete
//		attribute "wind_mph", "string"
//		attribute "wind_kph", "string"
//		attribute "wind_mps", "string"

		attribute "wind_degree", "string"
		attribute "wind_dir", "string"

//		attribute "pressure_mb", "string"
//		attribute "pressure_in", "string"

// not needed
//		attribute "precip_mm", "string"
//		attribute "precip_in", "string"

		attribute "precip_today", "string"

// not needed feelsLike covers this
//		attribute "cloud", "string"
//		attribute "feelslike_c", "string"
//		attribute "feelslike_f", "string"

		attribute "dewpoint", "string"
		attribute "visibility", "string"

// not needed (visibility covers these)
//		attribute "vis_km", "string"
//		attribute "vis_miles", "string"

		attribute "location", "string"
		attribute "city", "string"

// used by dashboards as eye candy
//		attribute "local_sunrise", "string"
//		attribute "local_sunset", "string"
//		attribute "twilight_begin", "string"
//		attribute "twilight_end", "string"

// obsolete covered by illuminance
//		attribute "illuminated", "string"

//not needed
//		attribute "cCF", "string"
//		attribute "lastXUupdate", "string"

		attribute "weather", "string"
		attribute "weatherIcon", "string"
		attribute "feelsLike", "string"
		attribute "wind", "string"

// not filled in by apiXU
//		attribute "percentPrecip", "string"

// used by dashboards - not needed
//		attribute "localSunrise", "string"
//		attribute "localSunset", "string"
//		attribute "visualDayPlus1", "string"
//		attribute "visualDayPlus1WithText", "string"
//		attribute "temperatureLowDayPlus1", "string"
//		attribute "temperatureHighDayPlus1", "string"
		attribute "temperatureLow", "string"
		attribute "temperatureHigh", "string"
//		attribute "forecastIcon", "string"
//		attribute "forecast_icon_url", "string"
//		attribute "forecast_text", "string"
//		attribute "forecast_code", "string"
//		attribute "wind_mytile", "string"
//		attribute "mytile", "string"

		attribute "apiXUquery", "string"

		command "refresh"
	}

	preferences	 {
		input "apixuKey", "text", title: "ApiXU key?", required: true
		input "zipCode", "text", title: "Override Zip code or set city name or latitude,longitude? (Default: ${location.zipCode})", defaultValue: null, required: false
		input "cityName", "text", title: "Override default city name?", required: false, defaultValue: null
//		input "isFahrenheit", "bool", title: "Use Imperial units?", required: false, defaultValue: true
//		input "publishWU", "bool", title: "Publish WU mappings?", required: true, defaultValue: false
//		input "dashClock", "bool", title: "Flash time ':' every 2 seconds?", required: false, defaultValue: false
		input "pollEvery", "enum", title: "Poll ApiXU how frequently?\nrecommended setting 30 minutes.\nilluminance is always updated every 5 minutes.", required: false, defaultValue: 30,
							options: [5:"5 minutes",10:"10 minutes",15:"15 minutes",30:"30 minutes"]
	}

}

def installed() {
	updated()
}

def initialize() {
	updated()
}

def updated() {
	if(apixuKey) {
		state.zipCode = settings?.zipCode ?: location.zipCode
		state.poll = settings?.pollEvery ?: 30
		state.wantMetric = settings?.isFahrenheit != null  ? !settings?.isFahrenheit :  (getTemperatureScale() == "C")
		unschedule()
		state.remove("tz_id")
		state.remove("today")

		//state.remove("cloud")
		//state.remove("clockSeconds")
		poll()
		"runEvery${state.poll}Minutes"(poll)
		runEvery5Minutes(updateLux)
//		schedule("0 * * * * ?", updateClock)
//		schedule("0/2 0 0 ? * * *", updateClock)
//		if (dashClock)  updateClock();
	} else { log.error "apixuKey not set" }
}

def wantMetric() {
	def t0 = state?.wantMetric == true ? true : false
	return t0
}

def poll() {
	def obs = getXUdata() // this is not async call
	if (!obs) {
		log.warn "No response from ApiXU API"
		return
	}
}

def finishPoll(obs) {
	if (!obs) {
		log.warn "No response from ApiXU API"
		return
	}
	log.debug ">>>>> apixu: Executing 'poll', location: ${state?.zipCode}"

	def now = new Date().format('yyyy-MM-dd HH:mm', location.timeZone)
//	sendEvent(name: "lastXUupdate", value: now, /* isStateChange: true, */ displayed: true)

	def tZ = TimeZone.getTimeZone(obs.location.tz_id)
	state.tz_id = obs.location.tz_id

	def localTime = new Date().parse("yyyy-MM-dd HH:mm", obs.location.localtime, tZ)
	def localDate = localTime.format("yyyy-MM-dd", tZ)
	def localTimeOnly = localTime.format("HH:mm", tZ)
	def todayDay = localTime.format("dd", tZ)

	if (!state?.today || state.today != todayDay) {
		state.today = todayDay
		def sunriseAndSunset = getSunriseAndSunset(obs.location.lat, obs.location.lon, localDate)
		def sunriseTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunriseAndSunset.results.sunrise, tZ)
		def sunsetTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunriseAndSunset.results.sunset, tZ)
		def noonTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunriseAndSunset.results.solar_noon, tZ)
		def twilight_begin = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunriseAndSunset.results.civil_twilight_begin, tZ)
		def twilight_end = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunriseAndSunset.results.civil_twilight_end, tZ)
		def localSunrise = sunriseTime.format("HH:mm", tZ)
//		sendEvent(name: "local_sunrise", value: localSunrise, descriptionText: "Sunrise today is at $localSunrise", /* isStateChange: true, */ displayed: true)
		def localSunset = sunsetTime.format("HH:mm", tZ)
//		sendEvent(name: "local_sunset", value: localSunset, descriptionText: "Sunset today at is $localSunset", /* isStateChange: true, */ displayed: true)
		def tB = twilight_begin.format("HH:mm", tZ)
//		sendEvent(name: "twilight_begin", value: tB, descriptionText: "Twilight begins today at $tB", /* isStateChange: true, */ displayed: true)
		def tE = twilight_end.format("HH:mm", tZ)
//		sendEvent(name: "twilight_end", value: tE, descriptionText: "Twilight ends today at $tE", /* isStateChange: true, */ displayed: true)

//		sendEvent(name: "localSunrise", value: localSunrise, /* isStateChange: true, */ displayed: true)
//		sendEvent(name: "localSunset", value: localSunset, /* isStateChange: true, */ displayed: true)

		state.sunriseTime = sunriseTime.format("yyyy-MM-dd'T'HH:mm:ssXXX", tZ)
		state.sunsetTime = sunsetTime.format("yyyy-MM-dd'T'HH:mm:ssXXX", tZ)
		state.noonTime = noonTime.format("yyyy-MM-dd'T'HH:mm:ssXXX", tZ)
		state.twilight_begin = twilight_begin.format("yyyy-MM-dd'T'HH:mm:ssXXX", tZ)
		state.twilight_end = twilight_end.format("yyyy-MM-dd'T'HH:mm:ssXXX", tZ)
	}

//	sendEvent(name: "name", value: obs.location.name, /* isStateChange: true, */ displayed: true)
	sendEvent(name: "region", value: obs.location.region, /* isStateChange: true, */ displayed: true)
	sendEvent(name: "country", value: obs.location.country, /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "lat", value: obs.location.lat, /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "lon", value: obs.location.lon, /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "tz_id", value: obs.location.tz_id, /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "localtime_epoch", value: obs.location.localtime_epoch, /* isStateChange: true, */ displayed: false)
//	sendEvent(name: "local_time", value: localTimeOnly, /* isStateChange: true, */ displayed: false)
//	sendEvent(name: "local_date", value: localDate, /* isStateChange: true, */ displayed: false)
//	sendEvent(name: "last_updated_epoch", value: obs.current.last_updated_epoch, /* isStateChange: true, */ displayed: true)
	if(isStateChange(device, "last_updated", obs.current.last_updated.toString())) {	
		sendEvent(name: "last_updated", value: obs.current.last_updated, isStateChange: true, displayed: true)
	}
//	sendEvent(name: "temp_c", value: obs.current.temp_c, unit: "C")
//	sendEvent(name: "temp_f", value: obs.current.temp_f, unit: "F")
	def t0 = (!wantMetric() ? obs.current.temp_f : obs.current.temp_c)
	sendEvent(name: "temperature", value: t0, unit: "${(!wantMetric() ? 'F' : 'C')}", /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "is_day", value: obs.current.is_day, /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "condition_text", value: obs.current.condition.text, /* isStateChange: true, */ displayed: true)    // same as "weather" below
//	sendEvent(name: "condition_icon", value: '<img src=https:' + obs.current.condition.icon + '>', /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "condition_icon_url", value: 'https:' + obs.current.condition.icon, /* isStateChange: true, */ displayed: true)
	sendEvent(name: "condition_code", value: obs.current.condition.code, /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "condition_icon_only", value: obs.current.condition.icon.split("/")[-1], /* isStateChange: true, */ displayed: true)
	def imgName = getImgName(obs.current.condition.code, obs.current.is_day)
//	sendEvent(name: "visual", value: '<img src=' + imgName + '>', /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "visualWithText", value: '<img src=' + imgName + '><br>' + obs.current.condition.text, /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "wind_mph", value: obs.current.wind_mph, unit: "MPH", /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "wind_kph", value: obs.current.wind_kph, unit: "KPH", /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "wind_mps", value: ((obs.current.wind_kph / 3.6f).round(1)), unit: "MPS", /* isStateChange: true, */ displayed: true)
	sendEvent(name: "wind_degree", value: obs.current.wind_degree, unit: "DEGREE", /* isStateChange: true, */ displayed: true)
	sendEvent(name: "wind_dir", value: obs.current.wind_dir, /* isStateChange: true, */ displayed: true)
	sendEvent(name: "ultravioletIndex", value: obs.current.uv, /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "pressure_mb", value: obs.current.pressure_mb, unit: "MBAR")
//	sendEvent(name: "pressure_in", value: obs.current.pressure_in, unit: "IN")
	sendEvent(name: "pressure", value: (!wantMetric() ? obs.current.pressure_in : obs.current.pressure_mb), unit: "${(!wantMetric() ? 'IN' : 'MBAR')}", /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "precip_mm", value: obs.current.precip_mm, unit: "MM", /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "precip_in", value: obs.current.precip_in, unit: "IN", /* isStateChange: true, */ displayed: true)
	sendEvent(name: "humidity", value: obs.current.humidity, unit: "%", /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "cloud", value: obs.current.cloud, unit: "%", /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "feelslike_c", value: obs.current.feelslike_c, unit: "C", /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "feelslike_f", value: obs.current.feelslike_f, unit: "F", /* isStateChange: true, */ displayed: true)

	double hum = obs.current.humidity?.toString().replaceAll("\\%", "") as Double
	double Tc = Math.round(obs.current.feelslike_c as Double) as Double
	float curDew = estimateDewPoint(hum,Tc)
	if(obs.current.temp_c < curDew) { curDew = obs.current.temp_c }
	curDew = !wantMetric() ? (curDew * 9/5 + 32).round(1) : curDew
	sendEvent(name: "dewpoint", value: curDew, unit: "${(!wantMetric() ? 'F' : 'C')}", /* isStateChange: true, */ displayed: true)

//	sendEvent(name: "vis_km", value: obs.current.vis_km, unit: "KM", /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "vis_miles", value: obs.current.vis_miles, unit: "MILES", /* isStateChange: true, */ displayed: true)

	def myCity = cityName ?: obs.location.name
	sendEvent(name: "location", value: myCity + ', ' + obs.location.region, /* isStateChange: true, */ displayed: true)
	state.condition_code = obs.current.condition.code
	//state.cloud = obs.current.cloud
	updateLux()

//	if (publishWU) {
		sendEvent(name: "city", value: myCity, /* isStateChange: true, */ displayed: true)
		sendEvent(name: "weather", value: getWUIconName(obs.current.condition.code, 1), /* isStateChange: true, */ displayed: true)
		sendEvent(name: "weatherIcon", value: getWUIconNum(obs.current.condition.code), /* isStateChange: true, */ displayed: true)
//		sendEvent(name: "weather", value: obs.current.condition.text, /* isStateChange: true, */ displayed: true)
		sendEvent(name: "feelsLike", value: (!wantMetric() ? obs.current.feelslike_f : obs.current.feelslike_c), unit: "${(!wantMetric() ? 'F' : 'C')}", /* isStateChange: true, */ displayed: true)
		sendEvent(name: "wind", value: (!wantMetric() ? obs.current.wind_mph : obs.current.wind_kph), unit: "${(!wantMetric() ? 'MPH' : 'KPH')}", /* isStateChange: true, */ displayed: true)
		sendEvent(name: "visibility", value: (!wantMetric() ? obs.current.vis_miles : obs.current.vis_km), unit: "${(!wantMetric() ? 'MILES' : 'KM')}", /* isStateChange: true, */ displayed: true)
		sendEvent(name: "precip_today", value: (!wantMetric() ? obs.current.precip_in : obs.current.precip_mm), unit: "${(!wantMetric() ? 'IN' : 'MM')}", /* isStateChange: true, */ displayed: true)
//		sendEvent(name: "percentPrecip", value: (!wantMetric() ? obs.current.precip_in : obs.current.precip_mm), unit: "${(!wantMetric() ? 'IN' : 'MM')}", /* isStateChange: true, */ displayed: true)
//	}
//		sendEvent(name: "forecastIcon", value: getWUIconName(obs.forecast.forecastday[1].day.condition.code, 1), /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "forecast_icon_url", value: 'https:' + obs.forecast.forecastday[1].day.condition.icon, /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "forecast_text", value: obs.forecast.forecastday[1].day.condition.text, /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "forecast_code", value: obs.forecast.forecastday[1].day.condition.code, /* isStateChange: true, */ displayed: true)

//	imgName = getImgName(obs.forecast.forecastday[1].day.condition.code, 1)
//	sendEvent(name: "visualDayPlus1", value: '<img src=' + imgName + '>', /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "visualDayPlus1WithText", value: '<img src=' + imgName + '><br>' + obs.forecast.forecastday[1].day.condition.text, /* isStateChange: true, */ displayed: true)
	sendEvent(name: "temperatureHigh", value: (!wantMetric() ? obs.forecast.forecastday[0].day.maxtemp_f : obs.forecast.forecastday[1].day.maxtemp_c), unit: "${(!wantMetric() ? 'F' : 'C')}", /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "temperatureHighDayPlus1", value: (!wantMetric() ? obs.forecast.forecastday[1].day.maxtemp_f :
//							obs.forecast.forecastday[1].day.maxtemp_c), unit: "${(!wantMetric() ? 'F' : 'C')}", /* isStateChange: true, */ displayed: true)
	sendEvent(name: "temperatureLow", value: (!wantMetric() ? obs.forecast.forecastday[0].day.mintemp_f : obs.forecast.forecastday[1].day.mintemp_c), unit: "${(!wantMetric() ? 'F' : 'C')}", /* isStateChange: true, */ displayed: true)
//	sendEvent(name: "temperatureLowDayPlus1", value: (!wantMetric() ? obs.forecast.forecastday[1].day.mintemp_f :
//							obs.forecast.forecastday[1].day.mintemp_c), unit: "${(!wantMetric() ? 'F' : 'C')}", /* isStateChange: true, */ displayed: true)

//	def wind_mytile=(!wantMetric() ? "${Math.round(obs.current.wind_mph)}" + " mph " : "${Math.round(obs.current.wind_kph)}" + " kph ")

//	sendEvent(name: "wind_mytile", value: wind_mytile, /* isStateChange: true, */ displayed: true)

//	def mytext = myCity + ', ' + obs.location.region
//	mytext += '<br>' + (!wantMetric() ? "${Math.round(obs.current.temp_f)}" + '&deg;F ' : obs.current.temp_c + '&deg;C ') + obs.current.humidity + '%'
//	mytext += '<br>' + device.currentState("localSunrise")?.value + ' <img style="height:1em" src=https:' + obs.current.condition.icon + '> ' + device.currentState("localSunset")?.value
//	mytext += (wind_mytile == (!wantMetric() ? "0 mph " : "0 kph ") ? '<br> Wind is calm' : '<br>' + obs.current.wind_dir + ' ' + wind_mytile)
//	mytext += '<br>' + obs.current.condition.text

//	sendEvent(name: "mytile", value: mytext, /* isStateChange: true, */ displayed: true)

	return
}

def refresh()	 { poll() }

def configure()	 { poll() }

private getXUdata() {
	//def obs = [:]
	def myUri = "https://api.apixu.com/v1/forecast.json?key=${apixuKey}&q=${state?.zipCode}&days=7"
	def params = [ uri: myUri ]
	try {
		asynchttpGet('ahttpRequestHandler', params, [tt: 'finishPoll'])
/*
		httpGet(params)	{ resp ->
			if (resp?.data)	 obs << resp.data;
			else log.error "http call for ApiXU weather api did not return data: $resp";
		}
*/
	} catch (e) {
		log.error "http call failed for ApiXU weather api: $e"
		return false
	}
	sendEvent(name: "apiXUquery", value: myUri, /* isStateChange: true, */ displayed: true)
	return true
}

public ahttpRequestHandler(resp, callbackData) {
	def json = [:]
	def obs = [:]
	if ((resp.status == 200) && resp.data) {
		try {
			json = resp.getJson()
		} catch (all) {
			json = [:]
			return
		}
	} else {
		if(resp.hasError()) {
			log.error "http Response Status: ${resp.status}   error Message: ${resp.getErrorMessage()}"
			return
		}
		log.error "no data: ${resp.status}   resp.data: ${resp.data} resp.json: ${resp.json}"
		return
	}
	obs = json
	//log.debug "$obs"
	def t0 = callbackData.tt
	"${t0}"(obs)
}

private getSunriseAndSunset(latitude, longitude, forDate) {
	def params = [ uri: "https://api.sunrise-sunset.org/json?lat=$latitude&lng=$longitude&date=$forDate&formatted=0" ]
	def sunRiseAndSet = [:]
	try {
		httpGet(params)	{ resp -> sunRiseAndSet = resp.data }
	} catch (e) { log.error "http call failed for sunrise and sunset api: $e" }
	return sunRiseAndSet
}

private estimateDewPoint(double rh,double t) {
	double L = Math.log(rh/100)
	double M = 17.27 * t
	double N = 237.3 + t
	double B = (L + (M/N)) / 17.27
	double dp = (237.3 * B) / (1 - B)

	double dp1 = 243.04 * ( Math.log(rh / 100) + ( (17.625 * t) / (243.04 + t) ) ) / (17.625 - Math.log(rh / 100) - ( (17.625 * t) / (243.04 + t) ) )
	double ave = (dp + dp1)/2
	//log.debug "dp: ${dp.round(1)} dp1: ${dp1.round(1)} ave: ${ave.round(1)}"
	ave = dp1
	return ave.round(1)
}

def updateLux()	 {
	if (!state.sunriseTime || !state.sunsetTime || !state.noonTime || !state.twilight_begin || !state.twilight_end || !state.tz_id)
		return

	def tZ = TimeZone.getTimeZone(state.tz_id)
	def lT = new Date().format("yyyy-MM-dd'T'HH:mm:ssXXX", tZ)
	def localTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", lT, tZ)
	def sunriseTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", state.sunriseTime, tZ)
	def sunsetTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", state.sunsetTime, tZ)
	def noonTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", state.noonTime, tZ)
	def twilight_begin = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", state.twilight_begin, tZ)
	def twilight_end = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", state.twilight_end, tZ)
	def lux = estimateLux(localTime, sunriseTime, sunsetTime, noonTime, twilight_begin, twilight_end, state.condition_code, state.cloud, state.tz_id)
	sendEvent(name: "illuminance", value: lux, unit: "lux", /* isStateChange: true, */ displayed: true)
	//sendEvent(name: "illuminated", value: String.format("%,d lux", lux), /* isStateChange: true, */ displayed: true)
}

private estimateLux(localTime, sunriseTime, sunsetTime, noonTime, twilight_begin, twilight_end, condition_code, cloud, tz_id)	 {
//	log.debug "condition_code: $condition_code | cloud: $cloud"
//	log.debug "twilight_begin: $twilight_begin | twilight_end: $twilight_end | tz_id: $tz_id"
//	log.debug "localTime: $localTime | sunriseTime: $sunriseTime | noonTime: $noonTime | sunsetTime: $sunsetTime"

	def tZ = TimeZone.getTimeZone(tz_id)
	long lux = 0l
	boolean aFCC = true
	float l

	if (timeOfDayIsBetween(sunriseTime, noonTime, localTime, tZ))	  {
		//log.debug "between sunrise and noon"
		l = (((localTime.getTime() - sunriseTime.getTime()) * 10000f) / (noonTime.getTime() - sunriseTime.getTime()))
		lux = (l < 50f ? 50l : l.trunc(0) as long)
	}
	else if (timeOfDayIsBetween(noonTime, sunsetTime, localTime, tZ))	  {
		//log.debug "between noon and sunset"
		l = (((sunsetTime.getTime() - localTime.getTime()) * 10000f) / (sunsetTime.getTime() - noonTime.getTime()))
		lux = (l < 50f ? 50l : l.trunc(0) as long)
	}
	else if (timeOfDayIsBetween(twilight_begin, sunriseTime, localTime, tZ))	  {
		//log.debug "between sunrise and twilight"
		l = (((localTime.getTime() - twilight_begin.getTime()) * 50f) / (sunriseTime.getTime() - twilight_begin.getTime()))
		lux = (l < 10f ? 10l : l.trunc(0) as long)
	}
	else if (timeOfDayIsBetween(sunsetTime, twilight_end, localTime, tZ))	  {
		//log.debug "between sunset and twilight"
		l = (((twilight_end.getTime() - localTime.getTime()) * 50f) / (twilight_end.getTime() - sunsetTime.getTime()))
		lux = (l < 10f ? 10l : l.trunc(0) as long)
	}
	else if (!timeOfDayIsBetween(twilight_begin, twilight_end, localTime, tZ))	  {
		//log.debug "between non-twilight"
		lux = 5l
		aFCC = false
	}

	int cC = condition_code.toInteger()
	String cCT = ''
	float cCF
	if (aFCC)
		if (conditionFactor[cC])	{
			cCF = conditionFactor[cC][1]
			cCT = conditionFactor[cC][0]
		}
		else	{
			cCF = ((100 - (cloud.toInteger() / 3d)) / 100).round(1)
			cCT = 'using cloud cover'
		}
	else	{
		cCF = 1.0
		cCT = 'night time now'
	}

	lux = (lux * cCF) as long
	if(lux > 1100) {
		long t0 = (lux/800)
		lux = t0 * 800
	} else if(lux <= 1100 && lux > 400) {
		long t0 = (lux/400)
		lux = t0 * 400
	} else {
		lux = 5
	}
//	log.debug "condition: $cC | condition text: $cCT | condition factor: $cCF | lux: $lux"
//	sendEvent(name: "cCF", value: cCF, /* isStateChange: true, */ displayed: true)

	return lux
}

private timeOfDayIsBetween(fromDate, toDate, checkDate, timeZone)	 {
	return (!checkDate.before(fromDate) && !checkDate.after(toDate))
}

def updateClock() {
	runIn(2, updateClock)
	if (!state.tz_id)  return;
	if (!tz_id)	   return;
	def nowTime = new Date()
	def tZ = TimeZone.getTimeZone(state.tz_id)
	sendEvent(name: "local_time", value: nowTime.format("HH:mm", tZ), displayed: true)
	def localDate = nowTime.format("yyyy-MM-dd", tZ)
	if (localDate != state.localDate) {
		state.localDate = localDate
		sendEvent(name: "local_date", value: localDate, displayed: true)
	}
}

def getWUIconName(condition_code, is_day)	 {
	def cC = condition_code.toInteger()
	def wuIcon = (conditionFactor[cC] ? conditionFactor[cC][2] : '')
	if (is_day != 1 && wuIcon)	wuIcon = 'nt_' + wuIcon;
	return wuIcon
}

def getWUIconNum(wCode)	 {
	def imgItem = imgNames.find{ it.code == wCode }
	return (imgItem ? imgItem.img : '44')
}

@Field final Map	conditionFactor = [
	1000: ['Sunny', 1, 'sunny'],						1003: ['Partly cloudy', 0.8, 'partlycloudy'],
	1006: ['Cloudy', 0.6, 'cloudy'],					1009: ['Overcast', 0.5, 'cloudy'],
	1030: ['Mist', 0.5, 'fog'],						1063: ['Patchy rain possible', 0.8, 'chancerain'],
	1066: ['Patchy snow possible', 0.6, 'chancesnow'],			1069: ['Patchy sleet possible', 0.6, 'chancesleet'],
	1072: ['Patchy freezing drizzle possible', 0.4, 'chancesleet'],		1087: ['Thundery outbreaks possible', 0.2, 'chancetstorms'],
	1114: ['Blowing snow', 0.3, 'snow'],					1117: ['Blizzard', 0.1, 'snow'],
	1135: ['Fog', 0.2, 'fog'],						1147: ['Freezing fog', 0.1, 'fog'],
	1150: ['Patchy light drizzle', 0.8, 'rain'],				1153: ['Light drizzle', 0.7, 'rain'],
	1168: ['Freezing drizzle', 0.5, 'sleet'],				1171: ['Heavy freezing drizzle', 0.2, 'sleet'],
	1180: ['Patchy light rain', 0.8, 'rain'],				1183: ['Light rain', 0.7, 'rain'],
	1186: ['Moderate rain at times', 0.5, 'rain'],				1189: ['Moderate rain', 0.4, 'rain'],
	1192: ['Heavy rain at times', 0.3, 'rain'],				1195: ['Heavy rain', 0.2, 'rain'],
	1198: ['Light freezing rain', 0.7, 'sleet'],				1201: ['Moderate or heavy freezing rain', 0.3, 'sleet'],
	1204: ['Light sleet', 0.5, 'sleet'],					1207: ['Moderate or heavy sleet', 0.3, 'sleet'],
	1210: ['Patchy light snow', 0.8, 'flurries'],				1213: ['Light snow', 0.7, 'snow'],
	1216: ['Patchy moderate snow', 0.6, 'snow'],				1219: ['Moderate snow', 0.5, 'snow'],
	1222: ['Patchy heavy snow', 0.4, 'snow'],				1225: ['Heavy snow', 0.3, 'snow'],
	1237: ['Ice pellets', 0.5, 'sleet'],					1240: ['Light rain shower', 0.8, 'rain'],
	1243: ['Moderate or heavy rain shower', 0.3, 'rain'],			1246: ['Torrential rain shower', 0.1, 'rain'],
	1249: ['Light sleet showers', 0.7, 'sleet'],				1252: ['Moderate or heavy sleet showers', 0.5, 'sleet'],
	1255: ['Light snow showers', 0.7, 'snow'],				1258: ['Moderate or heavy snow showers', 0.5, 'snow'],
	1261: ['Light showers of ice pellets', 0.7, 'sleet'],			1264: ['Moderate or heavy showers of ice pellets',0.3, 'sleet'],
	1273: ['Patchy light rain with thunder', 0.5, 'tstorms'],		1276: ['Moderate or heavy rain with thunder', 0.3, 'tstorms'],
	1279: ['Patchy light snow with thunder', 0.5, 'tstorms'],		1282: ['Moderate or heavy snow with thunder', 0.3, 'tstorms']
]

private getImgName(wCode, is_day) {
	def url = "https://cdn.rawgit.com/adey/bangali/master/resources/icons/weather/"
	def imgItem = imgNames.find{ it.code == wCode && it.day == is_day }
	return (url + (imgItem ? imgItem.img : 'na') + '.png')
}

@Field final List imgNames = [
	[code: 1000, day: 1, img: '32', ],	// DAY - Sunny
	[code: 1003, day: 1, img: '30', ],	// DAY - Partly cloudy
	[code: 1006, day: 1, img: '28', ],	// DAY - Cloudy
	[code: 1009, day: 1, img: '26', ],	// DAY - Overcast
	[code: 1030, day: 1, img: '20', ],	// DAY - Mist
	[code: 1063, day: 1, img: '39', ],	// DAY - Patchy rain possible
	[code: 1066, day: 1, img: '41', ],	// DAY - Patchy snow possible
	[code: 1069, day: 1, img: '41', ],	// DAY - Patchy sleet possible
	[code: 1072, day: 1, img: '39', ],	// DAY - Patchy freezing drizzle possible
	[code: 1087, day: 1, img: '38', ],	// DAY - Thundery outbreaks possible
	[code: 1114, day: 1, img: '15', ],	// DAY - Blowing snow
	[code: 1117, day: 1, img: '16', ],	// DAY - Blizzard
	[code: 1135, day: 1, img: '21', ],	// DAY - Fog
	[code: 1147, day: 1, img: '21', ],	// DAY - Freezing fog
	[code: 1150, day: 1, img: '39', ],	// DAY - Patchy light drizzle
	[code: 1153, day: 1, img: '11', ],	// DAY - Light drizzle
	[code: 1168, day: 1, img: '8', ],	// DAY - Freezing drizzle
	[code: 1171, day: 1, img: '10', ],	// DAY - Heavy freezing drizzle
	[code: 1180, day: 1, img: '39', ],	// DAY - Patchy light rain
	[code: 1183, day: 1, img: '11', ],	// DAY - Light rain
	[code: 1186, day: 1, img: '39', ],	// DAY - Moderate rain at times
	[code: 1189, day: 1, img: '12', ],	// DAY - Moderate rain
	[code: 1192, day: 1, img: '39', ],	// DAY - Heavy rain at times
	[code: 1195, day: 1, img: '12', ],	// DAY - Heavy rain
	[code: 1198, day: 1, img: '8', ],	// DAY - Light freezing rain
	[code: 1201, day: 1, img: '10', ],	// DAY - Moderate or heavy freezing rain
	[code: 1204, day: 1, img: '5', ],	// DAY - Light sleet
	[code: 1207, day: 1, img: '6', ],	// DAY - Moderate or heavy sleet
	[code: 1210, day: 1, img: '41', ],	// DAY - Patchy light snow
	[code: 1213, day: 1, img: '18', ],	// DAY - Light snow
	[code: 1216, day: 1, img: '41', ],	// DAY - Patchy moderate snow
	[code: 1219, day: 1, img: '16', ],	// DAY - Moderate snow
	[code: 1222, day: 1, img: '41', ],	// DAY - Patchy heavy snow
	[code: 1225, day: 1, img: '16', ],	// DAY - Heavy snow
	[code: 1237, day: 1, img: '18', ],	// DAY - Ice pellets
	[code: 1240, day: 1, img: '11', ],	// DAY - Light rain shower
	[code: 1243, day: 1, img: '12', ],	// DAY - Moderate or heavy rain shower
	[code: 1246, day: 1, img: '12', ],	// DAY - Torrential rain shower
	[code: 1249, day: 1, img: '5', ],	// DAY - Light sleet showers
	[code: 1252, day: 1, img: '6', ],	// DAY - Moderate or heavy sleet showers
	[code: 1255, day: 1, img: '16', ],	// DAY - Light snow showers
	[code: 1258, day: 1, img: '16', ],	// DAY - Moderate or heavy snow showers
	[code: 1261, day: 1, img: '8', ],	// DAY - Light showers of ice pellets
	[code: 1264, day: 1, img: '10', ],	// DAY - Moderate or heavy showers of ice pellets
	[code: 1273, day: 1, img: '38', ],	// DAY - Patchy light rain with thunder
	[code: 1276, day: 1, img: '35', ],	// DAY - Moderate or heavy rain with thunder
	[code: 1279, day: 1, img: '41', ],	// DAY - Patchy light snow with thunder
	[code: 1282, day: 1, img: '18', ],	// DAY - Moderate or heavy snow with thunder
	[code: 1000, day: 0, img: '31', ],	// NIGHT - Clear
	[code: 1003, day: 0, img: '29', ],	// NIGHT - Partly cloudy
	[code: 1006, day: 0, img: '27', ],	// NIGHT - Cloudy
	[code: 1009, day: 0, img: '26', ],	// NIGHT - Overcast
	[code: 1030, day: 0, img: '20', ],	// NIGHT - Mist
	[code: 1063, day: 0, img: '45', ],	// NIGHT - Patchy rain possible
	[code: 1066, day: 0, img: '46', ],	// NIGHT - Patchy snow possible
	[code: 1069, day: 0, img: '46', ],	// NIGHT - Patchy sleet possible
	[code: 1072, day: 0, img: '45', ],	// NIGHT - Patchy freezing drizzle possible
	[code: 1087, day: 0, img: '47', ],	// NIGHT - Thundery outbreaks possible
	[code: 1114, day: 0, img: '15', ],	// NIGHT - Blowing snow
	[code: 1117, day: 0, img: '16', ],	// NIGHT - Blizzard
	[code: 1135, day: 0, img: '21', ],	// NIGHT - Fog
	[code: 1147, day: 0, img: '21', ],	// NIGHT - Freezing fog
	[code: 1150, day: 0, img: '45', ],	// NIGHT - Patchy light drizzle
	[code: 1153, day: 0, img: '11', ],	// NIGHT - Light drizzle
	[code: 1168, day: 0, img: '8', ],	// NIGHT - Freezing drizzle
	[code: 1171, day: 0, img: '10', ],	// NIGHT - Heavy freezing drizzle
	[code: 1180, day: 0, img: '45', ],	// NIGHT - Patchy light rain
	[code: 1183, day: 0, img: '11', ],	// NIGHT - Light rain
	[code: 1186, day: 0, img: '45', ],	// NIGHT - Moderate rain at times
	[code: 1189, day: 0, img: '12', ],	// NIGHT - Moderate rain
	[code: 1192, day: 0, img: '45', ],	// NIGHT - Heavy rain at times
	[code: 1195, day: 0, img: '12', ],	// NIGHT - Heavy rain
	[code: 1198, day: 0, img: '8', ],	// NIGHT - Light freezing rain
	[code: 1201, day: 0, img: '10', ],	// NIGHT - Moderate or heavy freezing rain
	[code: 1204, day: 0, img: '5', ],	// NIGHT - Light sleet
	[code: 1207, day: 0, img: '6', ],	// NIGHT - Moderate or heavy sleet
	[code: 1210, day: 0, img: '41', ],	// NIGHT - Patchy light snow
	[code: 1213, day: 0, img: '18', ],	// NIGHT - Light snow
	[code: 1216, day: 0, img: '41', ],	// NIGHT - Patchy moderate snow
	[code: 1219, day: 0, img: '16', ],	// NIGHT - Moderate snow
	[code: 1222, day: 0, img: '41', ],	// NIGHT - Patchy heavy snow
	[code: 1225, day: 0, img: '16', ],	// NIGHT - Heavy snow
	[code: 1237, day: 0, img: '18', ],	// NIGHT - Ice pellets
	[code: 1240, day: 0, img: '11', ],	// NIGHT - Light rain shower
	[code: 1243, day: 0, img: '12', ],	// NIGHT - Moderate or heavy rain shower
	[code: 1246, day: 0, img: '12', ],	// NIGHT - Torrential rain shower
	[code: 1249, day: 0, img: '5', ],	// NIGHT - Light sleet showers
	[code: 1252, day: 0, img: '6', ],	// NIGHT - Moderate or heavy sleet showers
	[code: 1255, day: 0, img: '16', ],	// NIGHT - Light snow showers
	[code: 1258, day: 0, img: '16', ],	// NIGHT - Moderate or heavy snow showers
	[code: 1261, day: 0, img: '8', ],	// NIGHT - Light showers of ice pellets
	[code: 1264, day: 0, img: '10', ],	// NIGHT - Moderate or heavy showers of ice pellets
	[code: 1273, day: 0, img: '47', ],	// NIGHT - Patchy light rain with thunder
	[code: 1276, day: 0, img: '35', ],	// NIGHT - Moderate or heavy rain with thunder
	[code: 1279, day: 0, img: '46', ],	// NIGHT - Patchy light snow with thunder
	[code: 1282, day: 0, img: '18', ]	// NIGHT - Moderate or heavy snow with thunder
]

//**********************************************************************************************************************
