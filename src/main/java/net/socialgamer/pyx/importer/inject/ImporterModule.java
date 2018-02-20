/**
 * Copyright (c) 2018, Andy Janata
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions
 *   and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list of
 *   conditions and the following disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.socialgamer.pyx.importer.inject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.google.inject.throwingproviders.ThrowingProviderBinder;

import net.socialgamer.pyx.importer.filetypes.ExcelFileType;
import net.socialgamer.pyx.importer.parsers.SheetParser;


public class ImporterModule extends AbstractModule {

  private final Properties props;

  public ImporterModule(final Properties props) {
    this.props = props;
  }

  @Override
  protected void configure() {
    install(ThrowingProviderBinder.forModule(this));
    install(new FactoryModuleBuilder().build(ExcelFileType.Factory.class));
    install(new FactoryModuleBuilder().build(SheetParser.Factory.class));

    Names.bindProperties(binder(), props);
    bind(Properties.class).toInstance(props);
  }

  @Provides
  @Singleton
  @SpecialCharacterReplacements
  public LinkedHashMap<String, String> provideSpecialCharacterReplacements() {
    // iteration order matters for this
    final LinkedHashMap<String, String> map = new LinkedHashMap<>();
    final int count = Integer.parseInt(props.getProperty("replace.count", "0"));
    for (int i = 0; i < count; i++) {
      final String from = props.getProperty(String.format("replace[%d].from", i), "");
      if (from.isEmpty()) {
        throw new RuntimeException(
            "Special character replacement index " + i + " not found or is empty.");
      }
      final String to = props.getProperty(String.format("replace[%d].to", i), "");
      map.put(from, to);
    }
    return map;
  }

  @Provides
  @Singleton
  @DeckNameReplacements
  public Map<String, String> provideDeckNameReplacements() {
    final Map<String, String> map = new HashMap<>();
    final int count = Integer.parseInt(props.getProperty("deckname.count", "0"));
    for (int i = 0; i < count; i++) {
      final String from = props.getProperty(String.format("deckname[%d].from", i), "");
      if (from.isEmpty()) {
        throw new RuntimeException(
            "Deck name replacement index " + i + " not found or is empty.");
      }
      final String to = props.getProperty(String.format("deckname[%d].to", i), "");
      map.put(from, to);
    }
    return map;
  }

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  public @interface SpecialCharacterReplacements {
    //
  }

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  public @interface DeckNameReplacements {
    //
  }
}
