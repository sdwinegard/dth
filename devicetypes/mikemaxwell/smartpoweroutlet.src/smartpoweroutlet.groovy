/*
 *	Derived from SmartThings SmartPower Outlet
 *  Copyright 2016 Mike Maxwell and SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy
 *  of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 */

metadata {
	// Automatically generated. Make future change here.
	definition (name: "smartPowerOutlet", namespace: "MikeMaxwell", author: "mike maxwell") {
		capability "Actuator"
		capability "Switch"
		capability "Power Meter"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"
		capability "Health Check"

		// indicates that device keeps track of heartbeat (in state.heartbeat)
		attribute "heartbeat", "string"

		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0B04,0B05", outClusters: "0019", manufacturer: "CentraLite",  model: "3200", deviceJoinName: "Outlet"
		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0B04,0B05", outClusters: "0019", manufacturer: "CentraLite",  model: "3200-Sgb", deviceJoinName: "Outlet"
		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0B04,0B05", outClusters: "0019", manufacturer: "CentraLite",  model: "4257050-RZHAC", deviceJoinName: "Outlet"
		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0B04,0B05", outClusters: "0019"
        fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0B04,0B05,FC03",outClusters: "0019", manufacturer: "CentraLite",  model: "3210-L", deviceJoinName: "Outlet"
 	}

	// simulator metadata
	simulator {
		// status messages
		status "on": "on/off: 1"
		status "off": "on/off: 0"

		// reply messages
		reply "zcl on-off on": "on/off: 1"
		reply "zcl on-off off": "on/off: 0"
	}

	preferences {
		section {
			image(name: 'educationalcontent', multiple: true, images: [
				"http://cdn.device-gse.smartthings.com/Outlet/US/OutletUS1.jpg",
				"http://cdn.device-gse.smartthings.com/Outlet/US/OutletUS2.jpg"
				])
       	}
        section{
        	input(
        		name			: "riMin"
            	,type			: "enum"
            	,title			: "Minimum reporting interval"
            	//,description	: "Type"
            	,required		: true
            	,options		:[["1":"1 Second (default)"],["5":"5 seconds"],["30":"30 Seconds"],["60":"1 Minute"],["1800":"30 Minutes"],["3600":"1 Hour"]]
                ,defaultValue	: "1"
        	)
       		input(
        		name			: "riMax"
            	,type			: "enum"
            	,title			: "Maximum reporting interval (must be greater than Minimum)"
            	//,description	: "Type"
            	,required		: true
            	,options		:[["60":"1 Minute"],["300":"5 Minutes"],["600":"10 Minutes (default)"],["900":"15 Minutes"],["1800":"30 Minutes"],["3600":"1 Hour"]]
                ,defaultValue	: "600"
        	)
       		input(
        		name			: "wChange"
            	,type			: "enum"
            	,title			: "Minimum change for demand change reporting"
            	//,description	: "Type"
            	,required		: true
            	,options		:[["1":"1 Watt"],["5":"5 Watts (default)"],["15":"15 Watts"],["30":"30 Watts"],["60":"60 Watts"],["100":"100 Watts"],["200":"200 Watts"],["500":"500 Watts"],["1200":"1.2 KW"]]
                ,defaultValue	: "5"
        	)
                
		}
	}

	// UI tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: 'On', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#53a7c0", nextState: "turningOff"
				attributeState "off", label: 'Off', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
				attributeState "turningOn", label: 'Turning On', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#53a7c0", nextState: "turningOff"
				attributeState "turningOff", label: 'Turning Off', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
			}
			tileAttribute ("power", key: "SECONDARY_CONTROL") {
				attributeState "power", label:'${currentValue} W'
			}
		}

		standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main "switch"
		details(["switch","refresh"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "description is $description"

	// save heartbeat (i.e. last time we got a message from device)
	state.heartbeat = Calendar.getInstance().getTimeInMillis()

	def finalResult = zigbee.getKnownDescription(description)

	//TODO: Remove this after getKnownDescription can parse it automatically
	if (!finalResult && description!="updated")
		finalResult = getPowerDescription(zigbee.parseDescriptionAsMap(description))

	if (finalResult) {
		log.info "final result = $finalResult"
		if (finalResult.type == "update") {
			log.info "$device updates: ${finalResult.value}"
		}
		else if (finalResult.type == "power") {
			def powerValue = (finalResult.value as Integer)/10
			sendEvent(name: "power", value: powerValue, descriptionText: '{{ device.displayName }} power is {{ value }} Watts', translatable: true )
			/*
				Dividing by 10 as the Divisor is 10000 and unit is kW for the device. AttrId: 0302 and 0300. Simplifying to 10
				power level is an integer. The exact power level with correct units needs to be handled in the device type
				to account for the different Divisor value (AttrId: 0302) and POWER Unit (AttrId: 0300). CLUSTER for simple metering is 0702
			*/
		}
		else {
			def descriptionText = finalResult.value == "on" ? '{{ device.displayName }} is On' : '{{ device.displayName }} is Off'
			sendEvent(name: finalResult.type, value: finalResult.value, descriptionText: descriptionText, translatable: true)
		}
	}
	else {
		log.warn "DID NOT PARSE MESSAGE for description : $description"
		log.debug zigbee.parseDescriptionAsMap(description)
	}
}

def off() {
	zigbee.off()
}

def on() {
	zigbee.on()
}

def refresh() {
	sendEvent(name: "heartbeat", value: "alive", displayed:false)
	zigbee.onOffRefresh() + zigbee.refreshData("0x0B04", "0x050B")
}

def configure() {
	sendEvent(name: "checkInterval", value: 1200, displayed: false)
	zigbee.onOffConfig() + powerConfig() + refresh()
}

def powerConfig() {
    //old call style
	[
		"zdo bind 0x${device.deviceNetworkId} 1 ${endpointId} 0x0B04 {${device.zigbeeId}} {}", "delay 200",
		"zcl global send-me-a-report 0x0B04 0x050B 0x29 1 300 {05 00}",				//The send-me-a-report is custom to the attribute type for CentraLite
		"send 0x${device.deviceNetworkId} 1 ${endpointId}", "delay 500"
	]
    /*
    //new call style
    def riMin = (settings.riMin ?: 1).toInteger()
    def riMax = (settings.riMax ?: 600).toInteger()
    def wChange = (settings.wChange ?: 5).toInteger() //zigbee.convertToHexString((), 4)
    def wcHex = zigbee.convertToHexString(wChange, 4)
    log.debug "riMin: ${riMin}, riMax: ${riMax}, wChange: ${wChange}, wcHex: ${wcHex}"
    //new call
	zigbee.configureReporting(0x0B04, 0x050B, 0x29, riMin, riMax, wcHex)
    */
}

private getEndpointId() {
	new BigInteger(device.endpointId, 16).toString()
}

//TODO: Remove this after getKnownDescription can parse it automatically
def getPowerDescription(descMap) {
	def powerValue = "undefined"
	if (descMap.cluster == "0B04") {
		if (descMap.attrId == "050b") {
			if(descMap.value!="ffff")
				powerValue = zigbee.convertHexToInt(descMap.value)
		}
	}
	else if (descMap.clusterId == "0B04") {
		if(descMap.command=="07"){
			return	[type: "update", value : "power (0B04) capability configured successfully"]
		}
	}

	if (powerValue != "undefined"){
		return	[type: "power", value : powerValue]
	}
	else {
		return [:]
	}
}

//capture preference changes
def updated(){
    //log.debug "read: ${zigbee.readAttribute(0x0B04, 0x050B)}"
    //powerConfig()
    def riMin = (settings.riMin ?: 1).toInteger()
    def riMax = (settings.riMax ?: 600).toInteger()
    def wChange = (settings.wChange ?: 5).toInteger() //zigbee.convertToHexString((), 4)
    def wcHex = zigbee.convertToHexString(wChange, 4)
    //log.debug "read switch:${zigbee.readAttribute(0x0006, 0x0000)}"
    //log.debug "read power:${zigbee.readAttribute(0x0B04, 0x050B)}"
    //log.debug "riMin: ${riMin}, riMax: ${riMax}, wChange: ${wChange}, wcHex: ${wcHex}"
    
    //new call using config reporting
	//return 	zigbee.configureReporting(0x0B04, 0x050B, 0x29, riMin, riMax, wcHex)
    //new call using 
    //return zigbee.configSetup("0x0B04", "0x050B", "0x29", "${riMin}", "${riMax}", "${wcHex}")
    
    //return zigbee.readAttribute(0x0B04, 0x050B)
    return [
		"zdo bind 0x${device.deviceNetworkId} 1 ${endpointId} 0x0B04 {${device.zigbeeId}} {}", "delay 200",
		"zcl global send-me-a-report 0x0B04 0x050B 0x29 ${riMin} ${riMax} {05 00}",
        //if the above works, try the below
        //"zcl global send-me-a-report 0x0B04 0x050B 0x29 ${riMin} ${riMax} {${wcHex}}",
		"send 0x${device.deviceNetworkId} 1 ${endpointId}", "delay 500"
    ]
	//zigbee.configSetup(cluster, attributeId, dataType, minReportTime, maxReportTime, reportableChange)
	/**
	 * @param cluster - The cluster id of the requested report.
	 * @param attributeId - The attribute id for requested report.
	 * @param dataType - The two byte ZigBee type value for the requested report.
	 * @param minReportTime - Minimum number of seconds between reports.
	 * @param maxReportTime - Maximum number of seconds between reports.
	 * @param reportableChange - OCTET_STRING - Amount of change to trigger a report in curly braces. Empty curly braces if no change needs to be configured.
	 */
    
}