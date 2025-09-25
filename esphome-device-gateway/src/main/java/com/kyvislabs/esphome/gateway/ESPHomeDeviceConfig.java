package com.kyvislabs.esphome.gateway;

import com.inductiveautomation.ignition.gateway.dataroutes.openapi.annotations.DefaultValue;
import com.inductiveautomation.ignition.gateway.dataroutes.openapi.annotations.Description;
import com.inductiveautomation.ignition.gateway.dataroutes.openapi.annotations.FormCategory;
import com.inductiveautomation.ignition.gateway.dataroutes.openapi.annotations.FormField;
import com.inductiveautomation.ignition.gateway.dataroutes.openapi.annotations.Label;
import com.inductiveautomation.ignition.gateway.dataroutes.openapi.annotations.Required;
import com.inductiveautomation.ignition.gateway.web.nav.FormFieldType;

public record ESPHomeDeviceConfig(General general) {

  record General(
      @FormCategory("GENERAL")
          @Label("URL")
          @FormField(FormFieldType.TEXT)
          @DefaultValue("http://ipaddress/events")
          @Required
          @Description("The URL of the device to connect to.")
          String deviceUrl) {}
}
