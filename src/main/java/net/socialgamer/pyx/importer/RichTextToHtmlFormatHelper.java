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

package net.socialgamer.pyx.importer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.socialgamer.pyx.importer.inject.ImporterModule.SpecialCharacterReplacements;


@Singleton
public class RichTextToHtmlFormatHelper {

  private static final Logger LOG = Logger.getLogger(RichTextToHtmlFormatHelper.class);

  // log about any cards that have any characters above this left in them after html entity
  // replacement
  private static final char LAST_ASCII_CHARACTER = '~';

  /**
   * Replace these characters and character sequences with HTML entities or tags. Iteration order
   * matters.
   */
  private final Map<String, String> replacements;

  @Inject
  public RichTextToHtmlFormatHelper(
      @SpecialCharacterReplacements final LinkedHashMap<String, String> replacements) {
    this.replacements = ImmutableMap.copyOf(replacements);
  }

  public String format(final XSSFRichTextString rtf) {
    final String formatted;
    if (rtf.hasFormatting()) {
      LOG.trace(String.format("Processing formatting for %s", rtf.getString()));
      final StringBuilder builder = new StringBuilder();
      for (int i = 0; i < rtf.numFormattingRuns(); i++) {
        final String segment = replaceSpecials(
            rtf.getString().substring(rtf.getIndexOfFormattingRun(i),
                rtf.getIndexOfFormattingRun(i) + rtf.getLengthOfFormattingRun(i)));
        // will be null for normal font
        final XSSFFont font = rtf.getFontOfFormattingRun(i);
        if (null == font) {
          builder.append(segment);
        } else {
          int formatsApplied = 0;
          // figure out how to format it
          if (font.getBold()) {
            formatsApplied++;
            builder.append("<b>");
          }
          if (font.getItalic()) {
            formatsApplied++;
            builder.append("<i>");
          }
          if (font.getUnderline() > 0) {
            formatsApplied++;
            builder.append("<u>");
          }

          builder.append(segment);

          // reverse order
          if (font.getUnderline() > 0) {
            builder.append("</u>");
          }
          if (font.getItalic()) {
            builder.append("</i>");
          }
          if (font.getBold()) {
            builder.append("</b>");
          }

          // there might still be unknown formatting, if it also had something else...
          // TODO add a config option to not actually do any format processing so this can always trigger?
          if (0 == formatsApplied) {
            LOG.warn(String.format("Unknown formatting applied to segment '%s' of card '%s'.",
                segment, rtf.getString()));
          }
        }
      }
      formatted = builder.toString();
    } else {
      formatted = replaceSpecials(rtf.getString());
    }

    final String done = normalizeBlanks(formatted).trim();
    if (!done.equals(rtf.getString().trim())) {
      LOG.trace(String.format("Adjusted input string '%s' to '%s'.", rtf.getString(), done));
    }
    return done;
  }

  private String replaceSpecials(final String str) {
    String specialsReplaced = str;
    for (final Entry<String, String> entry : replacements.entrySet()) {
      if (specialsReplaced.contains(entry.getKey())) {
        specialsReplaced = specialsReplaced.replace(entry.getKey(), entry.getValue());
      }
    }

    // see if there are any we don't know about
    // TODO we might want to fail spectacularly here
    for (int i = 0; i < specialsReplaced.length(); i++) {
      if (specialsReplaced.charAt(i) > LAST_ASCII_CHARACTER) {
        LOG.warn(String.format("Unhandled special character '%c' in string '%s'.",
            specialsReplaced.charAt(i), str));
      }
    }
    return specialsReplaced;
  }

  private String normalizeBlanks(final String str) {
    // replace more than 4 underscores with 4 underscores
    return str.replaceAll("____+", "____");
  }
}
