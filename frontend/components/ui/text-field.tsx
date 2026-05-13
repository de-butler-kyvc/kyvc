"use client";

import * as React from "react";

import { cn } from "@/lib/utils";

import { Input } from "./input";

export type TextFieldProps = Omit<React.InputHTMLAttributes<HTMLInputElement>, "id"> & {
  id?: string;
  label: string;
  required?: boolean;
  error?: string;
  hint?: string;
  containerClassName?: string;
  endAdornment?: React.ReactNode;
};

export const TextField = React.forwardRef<HTMLInputElement, TextFieldProps>(
  (
    { id, name, label, required, error, hint, containerClassName, endAdornment, className, ...rest },
    ref
  ) => {
    const inputId = id ?? `tf-${name ?? label}`;
    const input = (
      <Input
        ref={ref}
        id={inputId}
        name={name}
        aria-invalid={!!error}
        className={cn(error && "error", className)}
        {...rest}
      />
    );
    return (
      <label className={cn("field", containerClassName)} htmlFor={inputId}>
        <span className="field-label">
          {label}
          {required ? <span style={{ color: "var(--danger)" }}> *</span> : null}
        </span>
        {endAdornment ? (
          <div className="form-row">
            <div style={{ flex: 1 }}>{input}</div>
            {endAdornment}
          </div>
        ) : (
          input
        )}
        {error ? (
          <span className="field-error">{error}</span>
        ) : hint ? (
          <span className="field-help">{hint}</span>
        ) : null}
      </label>
    );
  }
);
TextField.displayName = "TextField";
