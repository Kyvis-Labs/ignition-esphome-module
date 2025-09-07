package com.kyvislabs.esphome.gateway;

import com.inductiveautomation.ignition.gateway.config.ValidationErrors.Builder;
import com.inductiveautomation.ignition.gateway.dataroutes.openapi.SchemaUtil;
import com.inductiveautomation.ignition.gateway.opcua.server.api.Device;
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceContext;
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceExtensionPoint;
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceProfileConfig;
import com.inductiveautomation.ignition.gateway.web.nav.ExtensionPointResourceForm;
import com.inductiveautomation.ignition.gateway.web.nav.WebUiComponent;
import java.util.Optional;
import java.util.Set;

public class ESPHomeDeviceExtensionPoint extends DeviceExtensionPoint<ESPHomeDeviceConfig> {

  public static final String TYPE_ID = "ia.io.ExampleDevice";

  public ESPHomeDeviceExtensionPoint() {
    super(
        TYPE_ID,
        "ExampleDevice.Meta.DisplayName",
        "ExampleDevice.Meta.Description",
        ESPHomeDeviceConfig.class);
  }

  @Override
  protected Device createDevice(
      DeviceContext context, DeviceProfileConfig profileConfig, ESPHomeDeviceConfig deviceConfig) {

    return new ESPHomeDevice(context, deviceConfig);
  }

  @Override
  public Optional<WebUiComponent> getWebUiComponent(ComponentType type) {
    return Optional.of(
        new ExtensionPointResourceForm(
            DeviceExtensionPoint.DEVICE_RESOURCE_TYPE,
            "Device Connection",
            TYPE_ID,
            SchemaUtil.fromType(DeviceProfileConfig.class),
            SchemaUtil.fromType(ESPHomeDeviceConfig.class),
            Set.of()));
  }

  @Override
  protected void validate(ESPHomeDeviceConfig config, Builder errors) {
    errors.check(!config.general().deviceUrl().isEmpty(), "Device URL is empty");
  }
}
