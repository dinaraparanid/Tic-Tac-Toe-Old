use std::{
    io::{Read, Write},
    net::TcpStream,
    result::Result,
};

use crate::utils::*;
use std::io::Error;

pub(crate) const PLAYER_IS_FOUND: u8 = 0;
pub(crate) const PLAYER_MOVED: u8 = 1;

pub(crate) struct ClientPlayer {
    stream: TcpStream,
}

impl ClientPlayer {
    #[inline]
    pub(crate) fn new(ip: String) -> std::io::Result<ClientPlayer> {
        Ok(ClientPlayer {
            stream: TcpStream::connect(format!("{}:1337", ip))?,
        })
    }

    #[inline]
    pub(crate) fn send_ready(&mut self) -> StackTraceResult {
        get_err_if_exists(self.stream.write(&[PLAYER_IS_FOUND]))
    }

    #[inline]
    pub(crate) fn send_move(&mut self, y: u8, x: u8) -> StackTraceResult {
        get_err_if_exists(self.stream.write(&[PLAYER_MOVED, y, x]))
    }

    #[inline]
    pub(crate) fn read_command(&mut self) -> StackTraceValueResult<u8> {
        let mut data = [0];

        handle_err_value(self.stream.read_exact(&mut data), unsafe {
            *data.get_unchecked(0)
        })
    }

    #[inline]
    pub(crate) fn read_role(&mut self) -> StackTraceValueResult<u8> {
        let mut data = [0];

        handle_err_value(self.stream.read_exact(&mut data), unsafe {
            *data.get_unchecked(0)
        })
    }

    #[inline]
    pub(crate) fn read_table(&mut self) -> StackTraceValueResult<[[u8; 3]; 3]> {
        let mut data = [0; 9];

        handle_err_callback(self.stream.read_exact(&mut data), || {
            let mut table = [[0; 3]; 3];
            let mut iter = data.iter();

            (0..=3).for_each(|i| {
                (0..=3).for_each(|q| unsafe {
                    *table.get_unchecked_mut(i).get_unchecked_mut(q) = *iter.next().unwrap()
                })
            });

            table
        })
    }
}
