/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cli;

public class CliException extends RuntimeException {
    public CliException(Throwable t) {
        super(t);
      }

      public CliException(String message) {
        super(message);
      }

      public CliException(String message, Throwable t) {
          super(message, t);
      }

}
