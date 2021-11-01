use std::{convert::TryFrom, hint::unreachable_unchecked};

#[derive(Clone, Copy)]
pub(crate) enum GameState {
    Continue,
    ServerVictory,
    ClientVictory,
}

impl Default for GameState {
    #[inline]
    fn default() -> Self {
        Self::Continue
    }
}

impl From<u8> for GameState {
    #[inline]
    fn from(state: u8) -> Self {
        match state {
            0 => Self::Continue,
            1 => Self::ServerVictory,
            2 => Self::ClientVictory,
            _ => unreachable!(),
        }
    }
}

impl GameState {
    #[inline]
    pub unsafe fn from_unchecked(state: u8) -> Self {
        match state {
            0 => Self::Continue,
            1 => Self::ServerVictory,
            2 => Self::ClientVictory,
            _ => unreachable_unchecked(),
        }
    }
}
