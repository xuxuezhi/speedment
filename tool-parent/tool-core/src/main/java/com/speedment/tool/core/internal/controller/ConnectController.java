/**
 *
 * Copyright (c) 2006-2017, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.speedment.tool.core.internal.controller;

import com.speedment.common.injector.annotation.Inject;
import com.speedment.generator.core.component.EventComponent;
import com.speedment.runtime.config.Dbms;
import com.speedment.runtime.config.exception.SpeedmentConfigException;
import com.speedment.runtime.core.component.DbmsHandlerComponent;
import com.speedment.runtime.core.component.PasswordComponent;
import com.speedment.runtime.core.db.DbmsType;
import com.speedment.runtime.core.internal.util.Settings;
import static com.speedment.runtime.core.util.DatabaseUtil.dbmsTypeOf;
import com.speedment.tool.config.DbmsProperty;
import com.speedment.tool.core.component.UserInterfaceComponent;
import static com.speedment.tool.core.component.UserInterfaceComponent.ReuseStage.USE_EXISTING_STAGE;
import com.speedment.tool.core.event.UIEvent;
import com.speedment.tool.core.exception.SpeedmentToolException;
import static com.speedment.tool.core.internal.controller.ToolbarController.ICON_SIZE;
import com.speedment.tool.core.internal.util.ConfigFileHelper;
import com.speedment.tool.core.internal.util.InjectionLoader;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import static java.util.stream.Collectors.toCollection;
import java.util.stream.Stream;
import javafx.beans.binding.Bindings;
import static javafx.beans.binding.Bindings.createBooleanBinding;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

/**
 *
 * @author Emil Forslund
 */
public final class ConnectController implements Initializable {
    
    private final static String 
        DEFAULT_HOST = "127.0.0.1",
        DEFAULT_USER = "root",
        DEFAULT_NAME = "db0";
    
    private @Inject UserInterfaceComponent userInterfaceComponent;
    private @Inject DbmsHandlerComponent dbmsHandlerComponent;
    private @Inject PasswordComponent passwordComponent;
    private @Inject ConfigFileHelper configFileHelper;
    private @Inject EventComponent eventComponent;
    private @Inject InjectionLoader loader;
    
    private @FXML Button buttonOpen;
    private @FXML TextField fieldHost;
    private @FXML TextField fieldPort;
    private @FXML ChoiceBox<String> fieldType;
    private @FXML TextField fieldName;
    private @FXML TextField fieldSchema;
    private @FXML TextField fieldUser;
    private @FXML PasswordField fieldPass;
    private @FXML Button buttonConnect;
    private @FXML HBox container;
    private @FXML StackPane openContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (Settings.inst().get("hide_open_option", true)) {
            container.getChildren().remove(openContainer);
        }

        buttonOpen.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.FOLDER_OPEN, ICON_SIZE));
        buttonConnect.setGraphic(GlyphsDude.createIcon(FontAwesomeIcon.SIGN_IN, ICON_SIZE));
        
        fieldType.setItems(
            getDbmsTypes()
                .collect(toCollection(FXCollections::observableArrayList))
        );
        
        final DbmsProperty dbms = userInterfaceComponent.projectProperty()
            .mutator().addNewDbms();
        
        final AtomicReference<String> generatedHost = new AtomicReference<>("");
        final AtomicReference<String> generatedUser = new AtomicReference<>("");
        final AtomicReference<String> generatedName = new AtomicReference<>("");
        
        fieldType.getSelectionModel().selectedItemProperty()
            .addListener((observable, old, typeName) -> {
                
            dbms.stringPropertyOf(Dbms.TYPE_NAME, () -> typeName)
                .setValue(typeName);
            
            if (!typeName.isEmpty()) {
                final DbmsType item = dbmsTypeOf(dbmsHandlerComponent, dbms);

                if (fieldHost.getText().isEmpty()
                ||  fieldHost.getText().equals(generatedHost.get())) {
                    fieldHost.textProperty().setValue(DEFAULT_HOST);
                    generatedHost.set(DEFAULT_HOST);
                }

                if (fieldUser.getText().isEmpty()
                ||  fieldUser.getText().equals(generatedUser.get())) {
                    fieldUser.textProperty().setValue(DEFAULT_USER);
                    generatedUser.set(DEFAULT_USER);
                }
                
                if (fieldName.getText().isEmpty()
                ||  fieldName.getText().equals(generatedName.get())) {
                    final String name = item.getDefaultDbmsName()
                        .orElse(DEFAULT_NAME);
                    
                    fieldName.textProperty().setValue(name);
                    generatedName.set(name);
                }

                fieldName.getTooltip().setText(item.getDbmsNameMeaning());
                
                dbms.portProperty().setValue(item.getDefaultPort());
                fieldPort.textProperty().setValue(
                    Integer.toString(item.getDefaultPort())
                );
            }
        });
        
        Bindings.bindBidirectional(
                fieldPort.textProperty(), 
                dbms.portProperty(), 
                new StringConverter<Number>() {
                    
            @Override
            public String toString(Number number) {
                if (number == null) {
                    try {
                        return Integer.toString(defaultPort(dbms));
                    } catch (SpeedmentConfigException ex) {
                        return "";
                    }
                } else {
                    return number.toString();
                }
            }

            @Override
            public Number fromString(String string) {
                if (string == null || "".equals(string.trim())) {
                    return defaultPort(dbms);
                } else return Integer.parseInt(string);
            }
        });
        
        try {
            // Find the prefered dbms-type
            final String prefered = Settings.inst().get(
                "last_known_dbtype",
                getDbmsTypes()
                    .findFirst()
                    .orElseThrow(() -> new SpeedmentToolException(
                        "Could not find any installed JDBC drivers. Make " + 
                        "sure to include at least one JDBC driver as a " + 
                        "dependency in the projects pom.xml-file under the " + 
                        "speedment-maven-plugin <plugin> tag."
                    ))
            );
            
            // If the prefered dbms-type isn't loaded, select the first one.
            if (getDbmsTypes().anyMatch(prefered::equals)) {
                
                fieldType.getSelectionModel().select(prefered);
                
                final int port = Integer.parseInt(
                    Settings.inst().get(
                        "last_known_port", 
                        Integer.toString(defaultPort(dbms))
                    )
                );
                
                final String host = Settings.inst().get("last_known_host", DEFAULT_HOST);
                final String user = Settings.inst().get("last_known_user", DEFAULT_USER);
                final String name = Settings.inst().get("last_known_name", defaultName(dbms));
                
                generatedHost.set(host);
                generatedUser.set(user);
                generatedName.set(name);
                
                fieldSchema.setText(Settings.inst().get("last_known_schema"));
                fieldPort.setText(Integer.toString(port));
                fieldHost.setText(host);
                fieldUser.setText(user);
                fieldName.setText(name);
            } else {
                fieldType.getSelectionModel().select(
                    getDbmsTypes().findFirst().get()
                );
            }
        } catch (final SpeedmentToolException ex) {
            userInterfaceComponent.showError(
                "Couldn't find any installed JDBC drivers", 
                ex.getMessage(), ex
            );
            
            throw ex;
        }
        
        dbms.ipAddressProperty().bindBidirectional(fieldHost.textProperty());
        dbms.nameProperty().bindBidirectional(fieldName.textProperty());
        dbms.usernameProperty().bindBidirectional(fieldUser.textProperty());
        
        buttonOpen.setOnAction(ev -> 
            userInterfaceComponent.openProject(USE_EXISTING_STAGE));
        
        buttonConnect.setOnAction(ev -> {
            
            // Register password in password component
            passwordComponent
                .put(fieldName.getText(), fieldPass.getText().toCharArray());
            
            userInterfaceComponent.projectProperty().nameProperty()
                .setValue(fieldSchema.getText());
            
            Settings.inst().set("last_known_schema", fieldSchema.getText());
            Settings.inst().set("last_known_dbtype", dbms.getTypeName());
            Settings.inst().set("last_known_host", fieldHost.getText());
            Settings.inst().set("last_known_port", fieldPort.getText());
            Settings.inst().set("last_known_user", fieldUser.getText());
            Settings.inst().set("last_known_name", fieldName.getText());
            
            if (configFileHelper.loadFromDatabase(dbms, fieldSchema.getText())) {
                Settings.inst().set("hide_open_option", false);
                loader.loadAndShow("Scene");
                eventComponent.notify(UIEvent.OPEN_MAIN_WINDOW);
            }
        });
        
        buttonConnect.disableProperty().bind(createBooleanBinding(
            () -> fieldHost.textProperty().isEmpty().get()
            ||    fieldPort.textProperty().isEmpty().get()
            ||    fieldType.getSelectionModel().isEmpty()
            ||    fieldName.textProperty().isEmpty().get()
            ||    fieldSchema.textProperty().isEmpty().get()
            ||    fieldUser.textProperty().isEmpty().get(), 

            fieldHost.textProperty(),
            fieldPort.textProperty(),
            fieldType.selectionModelProperty(),
            fieldName.textProperty(),
            fieldSchema.textProperty(),
            fieldUser.textProperty()
        ));
    }
    
    private Stream<String> getDbmsTypes() {
        return dbmsHandlerComponent
            .supportedDbmsTypes()
            .map(DbmsType::getName);
    }
    
    private int defaultPort(DbmsProperty dbms) {
        return dbmsTypeOf(dbmsHandlerComponent, dbms)
            .getDefaultPort();
    }
    
    private String defaultName(DbmsProperty dbms) {
        return dbmsTypeOf(dbmsHandlerComponent, dbms)
            .getDefaultDbmsName()
            .orElse(DEFAULT_NAME);
    }
}