package com.kyvislabs.esphome.gateway.nativeapi;

import com.inductiveautomation.ignition.gateway.dataroutes.openapi.annotations.DefaultValue;
import com.inductiveautomation.ignition.gateway.dataroutes.openapi.annotations.Description;
import com.inductiveautomation.ignition.gateway.dataroutes.openapi.annotations.FormCategory;
import com.inductiveautomation.ignition.gateway.dataroutes.openapi.annotations.FormField;
import com.inductiveautomation.ignition.gateway.dataroutes.openapi.annotations.Label;
import com.inductiveautomation.ignition.gateway.dataroutes.openapi.annotations.Required;
import com.inductiveautomation.ignition.gateway.web.nav.FormFieldType;

public record NativeApiDeviceConfig(General general) {

    record General(
        @FormCategory("GENERAL")
            @Label("Host")
            @FormField(FormFieldType.TEXT)
            @DefaultValue("192.168.1.100")
            @Required
            @Description("The hostname or IP address of the ESPHome device.")
            String host,

        @FormCategory("GENERAL")
            @Label("Port")
            @FormField(FormFieldType.TEXT)
            @DefaultValue("6053")
            @Required
            @Description("The Native API port of the ESPHome device.")
            int port
    ) {}
}
