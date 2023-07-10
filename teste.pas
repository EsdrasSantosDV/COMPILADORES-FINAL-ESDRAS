Program CalcularSalario
Var TempoEmAnos, ValorSalario,i : Integer;
Begin
  read(TempoEmAnos, ValorSalario);
  Write(TempoEmAnos);
  If ((TempoEmAnos * 10) + 5 <= (10 * 10) + 1) Then
  Begin
    If (TempoEmAnos > 1) then
      Begin
        ValorSalario := 100;
      End;
  End
  Else
  Begin
    ValorSalario := ValorSalario * 2;
  End;

  For ValorSalario := 5 to 50 do
  Begin
    Write(ValorSalario);
  End;

  While (i < 10) do
  Begin
    Write(i);
    i := i + 1;
  End;

  Repeat
    Write(i);
    i := i + 1;
  until (i >= 10);


End.