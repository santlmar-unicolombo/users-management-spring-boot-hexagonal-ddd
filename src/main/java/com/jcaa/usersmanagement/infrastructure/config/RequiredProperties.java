package com.jcaa.usersmanagement.infrastructure.config;

import java.util.Objects;
import lombok.experimental.UtilityClass;

/** Validación temprana (al arrancar) de propiedades obligatorias de las integraciones cloud. */
@UtilityClass
public class RequiredProperties {

  public String requireText(final String value, final String propertyName) {
    if (Objects.isNull(value) || value.isBlank()) {
      throw ConfigurationException.becauseRequiredPropertyIsMissing(propertyName);
    }
    return value.trim();
  }
}
