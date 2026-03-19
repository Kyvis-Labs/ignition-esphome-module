package com.kyvislabs.esphome.gateway.nativeapi;

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

public class NativeApiDeviceExtensionPoint extends DeviceExtensionPoint<NativeApiDeviceConfig> {

    public static final String TYPE_ID = "org.kyvislabs.esphome.nativeapi";

    public NativeApiDeviceExtensionPoint() {
        super(
            TYPE_ID,
            "NativeApiDevice.Meta.DisplayName",
            "NativeApiDevice.Meta.Description",
            NativeApiDeviceConfig.class);
    }

    @Override
    protected Device createDevice(
            DeviceContext context, DeviceProfileConfig profileConfig, NativeApiDeviceConfig deviceConfig) {

        return new NativeApiDevice(context, deviceConfig);
    }

    @Override
    public Optional<WebUiComponent> getWebUiComponent(ComponentType type) {
        return Optional.of(
            new ExtensionPointResourceForm(
                DeviceExtensionPoint.DEVICE_RESOURCE_TYPE,
                "Device Connection",
                TYPE_ID,
                SchemaUtil.fromType(DeviceProfileConfig.class),
                SchemaUtil.fromType(NativeApiDeviceConfig.class),
                Set.of()));
    }

    @Override
    protected void validate(NativeApiDeviceConfig config, Builder errors) {
        errors.check(!config.general().host().isEmpty(), "Host is empty");
        errors.check(config.general().port() > 0 && config.general().port() <= 65535, "Port must be between 1 and 65535");
    }
}
