"use client";

import * as React from "react";

import { cn } from "@/lib/utils";

import { Input } from "./input";
import { Label } from "./label";

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
  ({ id, name, label, required, error, hint, containerClassName, endAdornment, className, ...rest }, ref) => {
    const inputId = id ?? `tf-${name ?? label}`;
    const input = (
      <Input
        ref={ref}
        id={inputId}
        name={name}
        aria-invalid={!!error}
        className={cn(error && "border-destructive focus-visible:ring-destructive", className)}
        {...rest}
      />
    );
    return (
      <div className={cn("flex flex-col gap-1.5", containerClassName)}>
        <Label htmlFor={inputId} className="text-[13px] text-foreground">
          {label}
          {required ? <span className="ml-0.5 text-destructive">*</span> : null}
        </Label>
        {endAdornment ? (
          <div className="flex gap-2">
            <div className="flex-1">{input}</div>
            {endAdornment}
          </div>
        ) : (
          input
        )}
        {error ? (
          <p className="text-[12px] text-destructive">{error}</p>
        ) : hint ? (
          <p className="text-[12px] text-muted-foreground">{hint}</p>
        ) : null}
      </div>
    );
  }
);
TextField.displayName = "TextField";
