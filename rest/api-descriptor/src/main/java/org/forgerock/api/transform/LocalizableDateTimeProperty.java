/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.api.transform;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import org.forgerock.util.i18n.LocalizableString;

import io.swagger.models.properties.DateTimeProperty;
import io.swagger.models.properties.Property;

/**
 * Localizable {@link DateTimeProperty}.
 */
class LocalizableDateTimeProperty extends DateTimeProperty implements LocalizableProperty<Property> {
    private LocalizableString title;
    private LocalizableString description;
    private String defaultValue;

    @Override
    public LocalizableDateTimeProperty title(LocalizableString title) {
        this.title = title;
        return this;
    }

    @Override
    public LocalizableDateTimeProperty description(LocalizableString desc) {
        this.description = desc;
        return this;
    }

    @Override
    public LocalizableDateTimeProperty title(String title) {
        setTitle(title);
        return this;
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        this.title = new LocalizableString(title);
    }

    @Override
    public LocalizableDateTimeProperty description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public void setDescription(String description) {
        super.setDescription(description);
        this.description = new LocalizableString(description);
    }

    @Override
    public LocalizableString getLocalizableTitle() {
        return title;
    }

    @Override
    public LocalizableString getLocalizableDescription() {
        return description;
    }

    /**
     * Sets the default value.
     *
     * @param defaultValue Default value or {@code null}
     */
    public void setDefault(Date defaultValue) {
        if (defaultValue != null) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            this.defaultValue = dateFormat.format(defaultValue);
        }
    }

    /**
     * Sets the default value, which should be in ISO 8601 date-time format.
     *
     * @param defaultValue Default value or {@code null}
     */
    @Override
    public void setDefault(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Gets the default value.
     *
     * @return Default value or {@code null}
     */
    public String getDefault() {
        return defaultValue;
    }

    @Override
    public boolean equals(final Object o) {
        if (!super.equals(o)) {
            return false;
        }
        if (!(o instanceof LocalizableDateTimeProperty)) {
            return false;
        }
        final LocalizableDateTimeProperty other = (LocalizableDateTimeProperty) o;
        if (!Objects.equals(defaultValue, other.defaultValue)) {
            return false;
        }
        if (!Objects.equals(title, other.title)) {
            return false;
        }
        if (!Objects.equals(description, other.description)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), title, description, defaultValue);
    }

}
