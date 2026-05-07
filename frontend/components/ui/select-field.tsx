"use client";

import * as React from "react";

import { cn } from "@/lib/utils";

import { Label } from "./label";

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
    { id, name, label, required, error, options, placeholder, containerClassName, className, ...rest },
    ref
  ) => {
    const inputId = id ?? `sf-${name ?? label}`;
    return (
      <div className={cn("flex flex-col gap-1.5", containerClassName)}>
        <Label htmlFor={inputId} className="text-[13px] text-foreground">
          {label}
          {required ? <span className="ml-0.5 text-destructive">*</span> : null}
        </Label>
        <select
          ref={ref}
          id={inputId}
          name={name}
          aria-invalid={!!error}
          className={cn(
            "flex h-9 w-full rounded-md border border-input bg-card px-3 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring",
            error && "border-destructive focus:ring-destructive",
            className
          )}
          {...rest}
        >
          {placeholder !== undefined ? <option value="">{placeholder}</option> : null}
          {options.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
        {error ? <p className="text-[12px] text-destructive">{error}</p> : null}
      </div>
    );
  }
);
SelectField.displayName = "SelectField";
