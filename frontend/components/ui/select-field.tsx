"use client";

import * as React from "react";

import { cn } from "@/lib/utils";

export type SelectOption = { value: string; label: string };

export type SelectFieldProps = Omit<React.SelectHTMLAttributes<HTMLSelectElement>, "id"> & {
  id?: string;
  label: string;
  required?: boolean;
  error?: string;
  options: SelectOption[];
  placeholder?: string;
  containerClassName?: string;
};

export const SelectField = React.forwardRef<HTMLSelectElement, SelectFieldProps>(
  (
    {
      id,
      name,
      label,
      required,
      error,
      options,
      placeholder,
      containerClassName,
      className,
      ...rest
    },
    ref
  ) => {
    const inputId = id ?? `sf-${name ?? label}`;
    return (
      <label className={cn("field", containerClassName)} htmlFor={inputId}>
        <span className="field-label">
          {label}
          {required ? <span style={{ color: "var(--danger)" }}> *</span> : null}
        </span>
        <select
          ref={ref}
          id={inputId}
          name={name}
          aria-invalid={!!error}
          className={cn("input", error && "error", className)}
          {...rest}
        >
          {placeholder !== undefined ? <option value="">{placeholder}</option> : null}
          {options.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
        {error ? <span className="field-error">{error}</span> : null}
      </label>
    );
  }
);
SelectField.displayName = "SelectField";
