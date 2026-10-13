import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

/**
 * Tema do SGP: laranja da marca como única cor de ação.
 * No claro o laranja é o 600 (contraste com texto branco nos botões); no escuro, o 500.
 */
export const AristocrataPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '{orange.50}',
      100: '{orange.100}',
      200: '{orange.200}',
      300: '{orange.300}',
      400: '{orange.400}',
      500: '{orange.500}',
      600: '{orange.600}',
      700: '{orange.700}',
      800: '{orange.800}',
      900: '{orange.900}',
      950: '{orange.950}',
    },
    surface: {
      0: '#ffffff',
      50: '{slate.50}',
      100: '{slate.100}',
      200: '{slate.200}',
      300: '{slate.300}',
      400: '{slate.400}',
      500: '{slate.500}',
      600: '{slate.600}',
      700: '{slate.700}',
      800: '{slate.800}',
      900: '{slate.900}',
      950: '{slate.950}',
    },
    formField: {
      borderRadius: '10px',
    },
    colorScheme: {
      light: {
        primary: {
          color: '{orange.600}',
          contrastColor: '#ffffff',
          hoverColor: '{orange.700}',
          activeColor: '{orange.800}',
        },
        highlight: {
          background: '{orange.50}',
          focusBackground: '{orange.100}',
          color: '{orange.700}',
          focusColor: '{orange.800}',
        },
      },
      dark: {
        primary: {
          color: '{orange.500}',
          contrastColor: '{slate.950}',
          hoverColor: '{orange.400}',
          activeColor: '{orange.300}',
        },
        highlight: {
          background: 'color-mix(in srgb, {orange.400}, transparent 84%)',
          focusBackground: 'color-mix(in srgb, {orange.400}, transparent 76%)',
          color: 'rgba(255,255,255,.87)',
          focusColor: 'rgba(255,255,255,.87)',
        },
      },
    },
  },
  components: {
    button: {
      root: {
        borderRadius: '10px',
      },
    },
  },
});
