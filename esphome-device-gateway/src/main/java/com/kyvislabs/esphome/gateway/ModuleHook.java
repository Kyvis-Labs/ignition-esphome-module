package com.kyvislabs.esphome.gateway;

import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.gateway.config.migration.IdbMigrationStrategy;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.opcua.server.api.AbstractDeviceModuleHook;
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceExtensionPoint;
import com.kyvislabs.esphome.gateway.nativeapi.NativeApiDevice;
import com.kyvislabs.esphome.gateway.nativeapi.NativeApiDeviceExtensionPoint;

import java.util.List;

public class ModuleHook extends AbstractDeviceModuleHook {

  @Override
  public void setup(GatewayContext context) {
    super.setup(context);

    BundleUtil.get().addBundle(ESPHomeDevice.class);
    BundleUtil.get().addBundle(NativeApiDevice.class);
  }

  @Override
  public void startup(LicenseState activationState) {
    super.startup(activationState);
  }

  @Override
  public void shutdown() {
    super.shutdown();

    BundleUtil.get().removeBundle(ESPHomeDevice.class);
    BundleUtil.get().removeBundle(NativeApiDevice.class);
  }

  @Override
  protected List<DeviceExtensionPoint<?>> getDeviceExtensionPoints() {
    return List.of(new ESPHomeDeviceExtensionPoint(), new NativeApiDeviceExtensionPoint());
  }

  @Override
  public List<IdbMigrationStrategy> getRecordMigrationStrategies() {

    return List.of();
  }

    @Override
    public boolean isMakerEditionCompatible() {
        return true;
    }

    @Override
    public boolean isFreeModule() {
        return true;
    }
}
